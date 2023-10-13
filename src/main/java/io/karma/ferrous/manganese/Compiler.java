/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.SimpleFileVisitor;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.util.ExceptionUtils;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMBitWriter;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Main class for invoking a compilation, either programmatically
 * or via a command line interface through the given main entry point.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Compiler implements ANTLRErrorListener {
    private static final String[] IN_EXTENSIONS = {"ferrous", "fe"};
    private static final String OUT_EXTENSION = "bc";
    private static final ThreadLocal<Compiler> INSTANCE = ThreadLocal.withInitial(Compiler::new);

    private final ArrayList<CompileError> errors = new ArrayList<>();
    private CompileStatus status = CompileStatus.SKIPPED;
    private BufferedTokenStream tokenStream;

    private Target target = null;
    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;

    // @formatter:off
    private Compiler() {}
    // @formatter:on

    public static Compiler getInstance() {
        return INSTANCE.get();
    }

    private static List<Path> findCompilableFiles(final Path path) {
        final var files = new ArrayList<Path>();
        if (!Files.isDirectory(path)) {
            files.add(path);
            return files;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor(filePath -> {
                final var fileName = filePath.getFileName().toString();
                for (final var ext : IN_EXTENSIONS) {
                    if (!fileName.endsWith(String.format(".%s", ext))) {
                        continue;
                    }
                    files.add(filePath);
                    break;
                }
                return FileVisitResult.CONTINUE;
            }));
        }
        catch (Exception error) { /* swallow exception */ }
        return files;
    }

    private static void disassemble(final TranslationUnit unit) {
        Logger.INSTANCE.infoln("");
        Logger.INSTANCE.info(LLVMCore.LLVMPrintModuleToString(unit.getModule()));
        Logger.INSTANCE.infoln("");
    }

    private void resetCompilation() {
        status = CompileStatus.SKIPPED;
        tokenStream = null;
        errors.clear();
        target = null;
    }

    public BufferedTokenStream getTokenStream() {
        return tokenStream;
    }

    public CompileStatus getStatus() {
        return status;
    }

    public void setTokenView(final boolean tokenView, final boolean extendedTokenView) {
        this.tokenView = tokenView;
        this.extendedTokenView = extendedTokenView;
    }

    public void setDisassemble(final boolean disassemble) {
        this.disassemble = disassemble;
    }

    public void setReportParserWarnings(final boolean reportParserWarnings) {
        this.reportParserWarnings = reportParserWarnings;
    }

    public boolean shouldDisassemble() {
        return disassemble;
    }

    public boolean isTokenViewEnabled() {
        return tokenView;
    }

    public boolean reportsParserWarnings() {
        return reportParserWarnings;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    public ArrayList<CompileError> getErrors() {
        return errors;
    }

    public void reportError(final CompileError error, final CompileStatus status) {
        errors.add(error);
        this.status = this.status.worse(status);
    }

    public CompileResult compile(final Path in, @Nullable Path out) {
        final var inputFiles = findCompilableFiles(in);
        final var numFiles = inputFiles.size();
        var status = CompileStatus.SKIPPED;

        for (var i = 0; i < numFiles; ++i) {
            final var filePath = inputFiles.get(i);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(numFiles, i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(filePath.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on

            // Attempt to deduce the output file name from the input file name, this may throw an AIOOB
            final var fileName = filePath.getFileName().toString();
            final var lastDot = fileName.lastIndexOf('.');
            final var rawName = fileName.substring(0, lastDot);
            final var outFileName = String.format("%s.%s", rawName, OUT_EXTENSION);

            if (out == null) {
                // If the output path is null, derive it
                out = filePath.getParent().resolve(outFileName);
            }
            if (!out.getFileName().toString().contains(".")) { // Since isFile/isDirectory doesn't work reliably for non-existent files
                // If out is a directory, resolve the output file name
                out = out.resolve(outFileName);
            }

            final var outParentFile = out.getParent();
            if (!Files.exists(outParentFile)) {
                try {
                    Files.createDirectories(outParentFile);
                    Logger.INSTANCE.debugln("Created directory %s", outParentFile);
                }
                catch (Exception error) {
                    Logger.INSTANCE.errorln("Could not create directory at %s, skipping", outParentFile.toString());
                }
            }

            Logger.INSTANCE.debugln("Input: %s", filePath);
            Logger.INSTANCE.debugln("Output: %s", out);

            try (final var fis = Files.newInputStream(filePath)) {
                try (final var fos = Files.newOutputStream(out)) {
                    compile(fileName, Channels.newChannel(fis), Channels.newChannel(fos));
                    status = status.worse(this.status);
                }
            }
            catch (IOException e) {
                status = status.worse(CompileStatus.IO_ERROR);
            }
            catch (Throwable e) {
                status = status.worse(CompileStatus.UNKNOWN_ERROR);
                ExceptionUtils.handleError(e, Logger.INSTANCE::errorln);
            }
        }

        return new CompileResult(status, inputFiles, new ArrayList<>(errors));
    }

    public void compile(final String name, final ReadableByteChannel in, final WritableByteChannel out) {
        resetCompilation(); // Reset before each compilation

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            final var lexer = new FerrousLexer(charStream);
            tokenStream = new CommonTokenStream(lexer);

            if (tokenView) {
                tokenStream.fill();
                final var tokens = tokenStream.getTokens();
                for (final var token : tokens) {
                    final var text = token.getText();
                    if (!extendedTokenView && text.isBlank()) {
                        continue; // Skip blank tokens if extended mode is disabled
                    }
                    // @formatter:off
                    final var tokenType = lexer.getTokenTypeMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(token.getType()))
                        .findFirst()
                        .orElseThrow();
                    //Logger.INSTANCE.infoln("%06d: %s (%s)", token.getTokenIndex(), text, tokenType);
                    System.out.println(Ansi.ansi()
                        .fg(Color.BLUE)
                        .a(String.format("%06d", token.getTokenIndex()))
                        .a(Attribute.RESET)
                        .a(": ")
                        .fgBright(Color.CYAN)
                        .a(text)
                        .a(Attribute.RESET)
                        .a(' ')
                        .fg(Color.GREEN)
                        .a(tokenType)
                        .a(Attribute.RESET).toString());
                    // @formatter:on
                }
            }

            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(this);

            final var unit = new TranslationUnit(this, name);
            ParseTreeWalker.DEFAULT.walk(unit, parser.file()); // Walk the entire AST with the TU
            if (!status.isRecoverable()) {
                Logger.INSTANCE.errorln("Compilation is cancelled from hereon out, continuing to report errors");
                return;
            }
            if (disassemble) {
                disassemble(unit);
            }

            final var buffer = LLVMBitWriter.LLVMWriteBitcodeToMemoryBuffer(unit.getModule());
            Logger.INSTANCE.debugln("Wrote bitcode to memory at 0x%08X", buffer);
            if (buffer == NULL) {
                status = status.worse(CompileStatus.TRANSLATION_ERROR);
                unit.dispose();
                return;
            }
            final var size = (int) LLVMCore.LLVMGetBufferSize(buffer);
            final var address = LLVMCore.nLLVMGetBufferStart(buffer); // LWJGL codegen is a pita
            out.write(MemoryUtil.memByteBuffer(address, size));
            LLVMCore.LLVMDisposeMemoryBuffer(buffer);

            status = CompileStatus.SUCCESS;
            unit.dispose();
        }
        catch (IOException e) {
            status = status.worse(CompileStatus.IO_ERROR);
        }
        catch (Throwable t) {
            status = status.worse(CompileStatus.UNKNOWN_ERROR);
            ExceptionUtils.handleError(t, Logger.INSTANCE::fatalln);
        }
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                            final int charPositionInLine, final String msg, final RecognitionException e) {
        errors.add(new CompileError((Token) offendingSymbol, tokenStream, line, charPositionInLine));
        status = status.worse(CompileStatus.SYNTAX_ERROR);
    }

    @Override
    public void reportAmbiguity(final Parser recognizer, final DFA dfa, final int startIndex, final int stopIndex,
                                final boolean exact, final BitSet ambigAlts, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected ambiguity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportAttemptingFullContext(final Parser recognizer, final DFA dfa, final int startIndex,
                                            final int stopIndex, final BitSet conflictingAlts,
                                            final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected full context at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportContextSensitivity(final Parser recognizer, final DFA dfa, final int startIndex,
                                         final int stopIndex, final int prediction, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debugln("Detected abnormally high context sensitivity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }
}

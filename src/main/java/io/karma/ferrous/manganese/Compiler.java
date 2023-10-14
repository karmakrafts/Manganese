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

import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.translate.DDTranslationUnit;
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.SimpleFileVisitor;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import io.karma.kommons.function.Functions;
import io.karma.kommons.function.XRunnable;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
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
import org.lwjgl.system.MemoryStack;
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

import static org.lwjgl.llvm.LLVMAnalysis.LLVMReturnStatusAction;
import static org.lwjgl.llvm.LLVMAnalysis.LLVMVerifyModule;
import static org.lwjgl.llvm.LLVMCore.LLVMDisposeMemoryBuffer;
import static org.lwjgl.llvm.LLVMCore.LLVMGetBufferSize;
import static org.lwjgl.llvm.LLVMCore.LLVMPrintModuleToString;
import static org.lwjgl.llvm.LLVMCore.nLLVMDisposeMessage;
import static org.lwjgl.llvm.LLVMCore.nLLVMGetBufferStart;
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
    private CompilePass currentPass = CompilePass.NONE;
    private CompileStatus status = CompileStatus.SKIPPED;
    private FerrousLexer lexer;
    private BufferedTokenStream tokenStream;
    private FerrousParser parser;
    private FileContext fileContext;
    private TranslationUnit translationUnit;

    private Target target = null;
    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;
    private boolean saveBitcode = false;
    private boolean isVerbose = false;
    private String moduleName = null;

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

    private void disassemble() {
        Logger.INSTANCE.infoln("");
        Logger.INSTANCE.info(LLVMPrintModuleToString(translationUnit.getModule()));
        Logger.INSTANCE.infoln("");
    }

    private void resetCompilation() {
        status = CompileStatus.SKIPPED;
        tokenStream = null;
        fileContext = null;
        lexer = null;
        parser = null;
        translationUnit = null;
        errors.clear();
        target = null;
    }

    public void doOrReport(final ParserRuleContext context, final XRunnable<?> closure) {
        if (!isVerbose && !status.isRecoverable()) {
            return; // Don't report translation errors after we are unrecoverable
        }
        Functions.tryDo(closure, exception -> {
            if (exception instanceof TranslationException tExcept) {
                reportError(tExcept.getError(), CompileStatus.TRANSLATION_ERROR);
                return;
            }
            final var error = new CompileError(context.start);
            error.setAdditionalText(Utils.makeCompilerMessage(exception.getMessage()));
            reportError(error, CompileStatus.TRANSLATION_ERROR);
        });
    }

    public void doOrReport(final XRunnable<?> closure) {
        if (!isVerbose && !status.isRecoverable()) {
            return; // Don't report translation errors after we are unrecoverable
        }
        Functions.tryDo(closure, exception -> {
            if (exception instanceof TranslationException tExcept) {
                reportError(tExcept.getError(), CompileStatus.TRANSLATION_ERROR);
                return;
            }
            reportError(new CompileError(exception.getMessage()), CompileStatus.TRANSLATION_ERROR);
        });
    }

    public BufferedTokenStream getTokenStream() {
        return tokenStream;
    }

    public CompileStatus getStatus() {
        return status;
    }

    public CompilePass getCurrentPass() {
        return currentPass;
    }

    public void setTokenView(final boolean tokenView, final boolean extendedTokenView) {
        this.tokenView = tokenView;
        this.extendedTokenView = extendedTokenView;
    }

    public void setSaveBitcode(boolean saveBitcode) {
        this.saveBitcode = saveBitcode;
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

    public boolean shouldSaveBitcode() {
        return saveBitcode;
    }

    public boolean isVerbose() {
        return isVerbose;
    }

    public void setVerbose(boolean verbose) {
        isVerbose = verbose;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
    }

    public ArrayList<CompileError> getErrors() {
        return errors;
    }

    public void reportError(final CompileError error, final CompileStatus status) {
        if (tokenStream.size() == 0) {
            tokenStream.fill();
        }
        if (errors.contains(error)) {
            return; // Don't report duplicates
        }
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

            final var rawFileName = Utils.getRawFileName(filePath);
            final var outFileName = String.format("%s.%s", rawFileName, OUT_EXTENSION);
            if (moduleName == null || moduleName.isBlank()) {
                moduleName = rawFileName; // Derive module name if not present
            }

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
                    compile(rawFileName, Channels.newChannel(fis), Channels.newChannel(fos));
                    status = status.worse(this.status);
                }
            }
            catch (IOException error) {
                reportError(new CompileError(error.getMessage()), CompileStatus.IO_ERROR);
            }
            catch (Exception error) {
                reportError(new CompileError(error.getMessage()), CompileStatus.UNKNOWN_ERROR);
            }
        }

        return new CompileResult(status, inputFiles, new ArrayList<>(errors));
    }

    private boolean verifyModule(final long module) {
        try (final var stack = MemoryStack.stackPush()) {
            final var messageBuffer = stack.callocPointer(1);
            if (LLVMVerifyModule(module, LLVMReturnStatusAction, messageBuffer)) {
                final var message = messageBuffer.get(0);
                if (message != NULL) {
                    reportError(new CompileError(messageBuffer.getStringUTF8(0)), CompileStatus.VERIFY_ERROR);
                    nLLVMDisposeMessage(message);
                    return false;
                }
                reportError(new CompileError("Unknown verify error"), CompileStatus.VERIFY_ERROR);
                return false;
            }
            return true;
        }
    }

    private void printTokens(final FerrousLexer lexer) {
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

    private boolean checkStatus() {
        if (!status.isRecoverable()) {
            Logger.INSTANCE.errorln("Compilation is irrecoverable, continuing to report syntax errors");
            return false;
        }
        return true;
    }

    private void processTokens(final List<Token> tokens) {
        // TODO: implement here
    }

    private void tokenize(final CharStream stream) {
        final var startTime = System.currentTimeMillis();
        currentPass = CompilePass.TOKENIZE;
        final var lexer = new FerrousLexer(stream);
        tokenStream = new CommonTokenStream(lexer);
        tokenStream.fill();
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass TOKENIZE in %dms", time);
        if (tokenView) {
            printTokens(lexer);
        }
    }

    private void parse() {
        final var startTime = System.currentTimeMillis();
        currentPass = CompilePass.PARSE;
        parser = new FerrousParser(tokenStream);
        parser.removeErrorListeners(); // Remove default error listener
        parser.addErrorListener(this);
        fileContext = parser.file();
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass PARSE in %dms", time);
    }

    private boolean analyze() {
        final var startTime = System.currentTimeMillis();
        currentPass = CompilePass.ANALYZE;
        final var discoveryUnit = new DDTranslationUnit(this);
        ParseTreeWalker.DEFAULT.walk(discoveryUnit, fileContext);
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass ANALYZE in %dms", time);
        return checkStatus();
    }

    private void process() {
        final var startTime = System.currentTimeMillis();
        currentPass = CompilePass.PROCESS;
        final var tokens = tokenStream.getTokens();
        processTokens(tokens);
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass PROCESS in %dms", time);
        tokenStream = new CommonTokenStream(new ListTokenSource(tokens));
        parser.setTokenStream(tokenStream);
        parser.reset();
        parser.removeErrorListeners();
        parser.addErrorListener(this);
    }

    private boolean compile(final String name) {
        final var startTime = System.currentTimeMillis();
        currentPass = CompilePass.COMPILE;
        translationUnit = new TranslationUnit(this, name);
        ParseTreeWalker.DEFAULT.walk(translationUnit, fileContext); // Walk the entire AST with the TU
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass COMPILE in %dms", time);
        return checkStatus();
    }

    private void saveBitcode(final Path path) {
        final var module = translationUnit.getModule();
        final var buffer = LLVMBitWriter.LLVMWriteBitcodeToMemoryBuffer(module);
        Logger.INSTANCE.debugln("Wrote bitcode to memory at 0x%08X", buffer);
        if (buffer == NULL) {
            reportError(new CompileError("Could not allocate buffer for module bitcode"), CompileStatus.IO_ERROR);
            translationUnit.dispose();
            return;
        }

        try (final var stream = Files.newOutputStream(path)) {
            final var size = (int) LLVMGetBufferSize(buffer);
            final var address = nLLVMGetBufferStart(buffer); // LWJGL codegen is a pita
            Channels.newChannel(stream).write(MemoryUtil.memByteBuffer(address, size));
        }
        catch (Exception error) {
            Logger.INSTANCE.warnln("Could not save bitcode to file: %s", error.getMessage());
        }

        LLVMDisposeMemoryBuffer(buffer);
    }

    public void compile(final String name, final ReadableByteChannel in, final WritableByteChannel out) {
        resetCompilation(); // Reset before each compilation

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            if (charStream.size() == 0) {
                Logger.INSTANCE.warnln("No input data, skipping compilation");
                return;
            }

            tokenize(charStream);
            parse();
            if (!analyze()) {
                return;
            }
            process();
            if (!compile(name)) {
                return;
            }

            final var module = translationUnit.getModule();
            if (!verifyModule(module)) {
                return;
            }
            if (disassemble) {
                disassemble();
            }
            if (saveBitcode) {
                saveBitcode(Path.of(String.format("%s.bc", name)));
            }

            status = CompileStatus.SUCCESS;
            translationUnit.dispose();
            currentPass = CompilePass.NONE;
        }
        catch (IOException error) {
            reportError(new CompileError(error.getMessage()), CompileStatus.IO_ERROR);
        }
        catch (Exception error) {
            reportError(new CompileError(error.getMessage()), CompileStatus.UNKNOWN_ERROR);
        }
    }

    @Override
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                            final int charPositionInLine, final String msg, final RecognitionException e) {
        final var error = new CompileError((Token) offendingSymbol, tokenStream, line, charPositionInLine);
        error.setAdditionalText(Utils.makeCompilerMessage(Utils.capitalize(msg), null));
        reportError(error, CompileStatus.SYNTAX_ERROR);
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

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

import io.karma.ferrous.manganese.analyze.Analyzer;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ListTokenSource;
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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Compiler implements ANTLRErrorListener {
    private static final String[] IN_EXTENSIONS = {"ferrous", "fe"};

    private final ArrayList<CompileError> errors = new ArrayList<>();
    private final HashMap<String, Module> modules = new HashMap<>();
    private final TargetMachine targetMachine;

    private CompilePass currentPass = CompilePass.NONE;
    private String currentName;
    private CompileStatus status = CompileStatus.SKIPPED;
    private BufferedTokenStream tokenStream;
    private FerrousParser parser;
    private FileContext fileContext;
    private Analyzer analyzer;
    private TranslationUnit translationUnit;
    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;
    private boolean saveBitcode = false;
    private boolean isVerbose = false;
    private String moduleName = null;

    Compiler(final TargetMachine targetMachine) {
        this.targetMachine = targetMachine;
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
            Logger.INSTANCE.debugln("Detected abnormally high context sensitivity at %d:%d (%d)", startIndex, stopIndex,
                                    dfa.decision);
        }
    }

    private void resetCompilation() {
        status = CompileStatus.SKIPPED;
        currentName = null;
        tokenStream = null;
        fileContext = null;
        parser = null;
        analyzer = null;
        translationUnit = null;
        errors.clear();
    }

    public CompileError makeError(final Token token) {
        return new CompileError(token, tokenStream);
    }

    public CompileError makeError(final Token token, final String additionalText) {
        final var error = new CompileError(token, tokenStream);
        error.setAdditionalText(additionalText);
        return error;
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

    public CompileResult compile(final Path in, final Path out, final Path buildDir) {
        final var inputFiles = Utils.findFilesWithExtensions(in, IN_EXTENSIONS);
        final var numFiles = inputFiles.size();
        var result = new CompileResult(CompileStatus.SKIPPED);

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
            final var outFile = out.resolve(
                    String.format("%s.%s", rawFileName, targetMachine.getFileType().getExtension()));
            Logger.INSTANCE.debugln("Input: %s", filePath);
            Logger.INSTANCE.debugln("Output: %s", outFile);

            try (final var inStream = Files.newInputStream(filePath); final var inChannel = Channels.newChannel(
                    inStream)) {
                try (final var outStream = Files.newOutputStream(outFile); final var outChannel = Channels.newChannel(
                        outStream)) {
                    result = result.merge(compile(rawFileName, inChannel, outChannel));
                }
            }
            catch (IOException error) {
                reportError(new CompileError(error.toString()), CompileStatus.IO_ERROR);
                return new CompileResult(CompileStatus.IO_ERROR, Collections.emptyList(), new ArrayList<>(errors));
            }
            catch (Exception error) {
                reportError(new CompileError(error.toString()), CompileStatus.UNKNOWN_ERROR);
                return new CompileResult(CompileStatus.UNKNOWN_ERROR, Collections.emptyList(), new ArrayList<>(errors));
            }
        }

        result.compiledFiles().add(in);
        return result;
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
            System.out.printf("\n%s\n", TokenUtils.renderTokenTree(moduleName, extendedTokenView, lexer,
                                                                   tokenStream.getTokens()));
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
        analyzer = new Analyzer(this);
        ParseTreeWalker.DEFAULT.walk(analyzer, fileContext);
        analyzer.preProcessTypes(); // Pre-materializes all UDTs in the right order
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
        tokenStream.fill();
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

    public CompileResult compile(final String name, final ReadableByteChannel in, final WritableByteChannel out) {
        resetCompilation(); // Reset before each compilation
        currentName = name;

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            if (charStream.size() == 0) {
                Logger.INSTANCE.warnln("No input data, skipping compilation");
                return new CompileResult(CompileStatus.SKIPPED, Collections.emptyList(), new ArrayList<>(errors));
            }

            tokenize(charStream);
            parse();
            if (!analyze()) {
                return new CompileResult(CompileStatus.ANALYZER_ERROR, Collections.emptyList(),
                                         new ArrayList<>(errors));
            }
            process();
            if (!compile(name)) {
                return new CompileResult(CompileStatus.TRANSLATION_ERROR, Collections.emptyList(),
                                         new ArrayList<>(errors));
            }

            final var module = translationUnit.getModule();
            final var verificationStatus = module.verify();
            if (verificationStatus != null) {
                reportError(new CompileError(verificationStatus), CompileStatus.TRANSLATION_ERROR);
                return new CompileResult(CompileStatus.VERIFY_ERROR, Collections.emptyList(), new ArrayList<>(errors));
            }

            if (disassemble) {
                Logger.INSTANCE.infoln("");
                Logger.INSTANCE.info("%s", translationUnit.getModule().disassemble());
                Logger.INSTANCE.infoln("");
            }

            modules.put(currentName, module);
            status = CompileStatus.SUCCESS;
            currentPass = CompilePass.NONE;
            return new CompileResult(CompileStatus.SUCCESS);
        }
        catch (IOException error) {
            reportError(new CompileError(Utils.makeCompilerMessage(error.toString())), CompileStatus.IO_ERROR);
            return new CompileResult(CompileStatus.IO_ERROR, Collections.emptyList(), new ArrayList<>(errors));
        }
        catch (Exception error) {
            reportError(new CompileError(Utils.makeCompilerMessage(error.toString())), CompileStatus.UNKNOWN_ERROR);
            return new CompileResult(CompileStatus.UNKNOWN_ERROR, Collections.emptyList(), new ArrayList<>(errors));
        }
    }

    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
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

    public String getCurrentName() {
        return currentName;
    }

    public TranslationUnit getTranslationUnit() {
        return translationUnit;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
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

    public TargetMachine getTargetMachine() {
        return targetMachine;
    }

    public void dispose() {
        for (final var module : modules.values()) {
            module.dispose();
        }
        modules.clear();
        targetMachine.dispose();
    }
}

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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.analyze.Analyzer;
import io.karma.ferrous.manganese.target.FileType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.function.Functions;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.llvm.LLVMCore.LLVMContextSetOpaquePointers;
import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Compiler {
    private static final String[] IN_EXTENSIONS = {"ferrous", "fe"};

    private final TargetMachine targetMachine;
    private final ExecutorService executorService;
    private final AtomicInteger numRunningTasks = new AtomicInteger(0);

    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;

    @API(status = Status.INTERNAL)
    public Compiler(final TargetMachine targetMachine, final int numThreads) {
        this.targetMachine = targetMachine;
        executorService = Executors.newFixedThreadPool(numThreads);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Functions.tryDo(() -> {
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow().forEach(Runnable::run);
            }
        })));
    }

    public void tokenizeAndParse(final String name, final ReadableByteChannel in, final CompileContext context) {
        try {
            context.setCurrentModuleName(name);

            // Tokenize
            var startTime = System.currentTimeMillis();
            context.setCurrentPass(CompilePass.TOKENIZE);
            final var lexer = new FerrousLexer(CharStreams.fromChannel(in, StandardCharsets.UTF_8));
            context.setLexer(lexer);
            final var tokenStream = new CommonTokenStream(lexer);
            tokenStream.fill();
            context.setTokenStream(tokenStream);
            var time = System.currentTimeMillis() - startTime;
            Logger.INSTANCE.debugln("Finished pass TOKENIZE in %dms", time);
            if (tokenView) {
                System.out.printf("\n%s\n",
                        TokenUtils.renderTokenTree(context.getCurrentModuleName(), extendedTokenView, lexer,
                                tokenStream.getTokens()));
            }
            // Parse
            context.setCurrentPass(CompilePass.PARSE);
            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(new ErrorListener(context));
            context.setParser(parser);
            startTime = System.currentTimeMillis();
            context.setFileContext(parser.file());
            time = System.currentTimeMillis() - startTime;
            Logger.INSTANCE.debugln("Finished pass PARSE in %dms", time);

            context.setCurrentPass(CompilePass.NONE);
        } catch (IOException error) {
            context.setCurrentPass(CompilePass.NONE);
            context.reportError(context.makeError(CompileErrorCode.E0002));
        }
    }

    public void analyzeAndProcess(final String name, final CompileContext context) {
        context.setCurrentModuleName(name);
        analyze(context);
        process(context);
    }

    public void compile(final String name, final String sourceName, final WritableByteChannel out,
                        final CompileContext context) {
        context.setCurrentModuleName(name);
        compile(context);

        final var module = Objects.requireNonNull(context.getTranslationUnit()).getModule();
        module.setSourceFileName(sourceName);
        final var verificationStatus = module.verify();
        if (verificationStatus != null) {
            context.reportError(context.makeError(CompileErrorCode.E0002));
            return;
        }

        if (disassemble) {
            Logger.INSTANCE.infoln("");
            Logger.INSTANCE.info("%s", module.disassemble());
            Logger.INSTANCE.infoln("");
        }

        context.addModule(module);
        context.setCurrentPass(CompilePass.NONE);
    }

    public CompileResult compile(final Path in, final Path out, final FileType fileType, final CompileContext context) {
        final var inputFiles = Utils.findFilesWithExtensions(in, IN_EXTENSIONS);
        final var numFiles = inputFiles.size();
        final var maxProgress = numFiles << 1;

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(maxProgress, i))
                .a(Attribute.RESET)
                .a(" Analyzing file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = Utils.getRawFileName(file);

            numRunningTasks.incrementAndGet();
            executorService.submit(() -> {
                Logger.INSTANCE.debugln("Input: %s", file);
                try (final var stream = Files.newInputStream(file); final var channel = Channels.newChannel(stream)) {
                    tokenizeAndParse(rawFileName, channel, context);
                    analyzeAndProcess(rawFileName, context);
                } catch (IOException error) {
                    context.reportError(context.makeError(CompileErrorCode.E0003));
                }
                numRunningTasks.decrementAndGet();
            });
        }

        while (numRunningTasks.get() > 0) {
            Thread.yield();
        }

        final var moduleName = Utils.getRawFileName(in);
        final var module = targetMachine.createModule(moduleName);
        module.setSourceFileName(String.format("%s.%s", moduleName, fileType.getExtension()));

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(maxProgress, numFiles + i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = Utils.getRawFileName(file);

            try (final var stream = new ByteArrayOutputStream(); final var channel = Channels.newChannel(stream)) {
                compile(rawFileName, file.getFileName().toString(), channel, context);
                context.setCurrentPass(CompilePass.LINK);
                module.linkIn(context.getModule());
                context.setCurrentPass(CompilePass.NONE);
            } catch (IOException error) {
                context.reportError(context.makeError(CompileErrorCode.E0004));
            }
        }

        final var globalModule = Objects.requireNonNull(
                Functions.tryGet(() -> targetMachine.loadEmbeddedModule("global", LLVMGetGlobalContext())));
        module.linkIn(globalModule);
        globalModule.dispose();

        if (disassemble) {
            Logger.INSTANCE.infoln("\nLinked disassembly:\n\n%s", module.disassemble());
            Logger.INSTANCE.infoln("Native disassembly:\n\n%s\n", module.disassembleASM(targetMachine));
        }
        module.dispose();

        return context.makeResult();
    }

    public void setEnableOpaquePointers(boolean enableOpaquePointers) {
        LLVMContextSetOpaquePointers(LLVMGetGlobalContext(), enableOpaquePointers);
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

    public TargetMachine getTargetMachine() {
        return targetMachine;
    }

    private void processTokens(final List<Token> tokens) {
        // TODO: implement here
    }

    private void analyze(final CompileContext context) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.ANALYZE);
        final var analyzer = new Analyzer(this, context);
        context.setAnalyzer(analyzer);
        final var fileContext = Objects.requireNonNull(context.getFileContext());
        ParseTreeWalker.DEFAULT.walk(analyzer, fileContext);
        analyzer.preProcessTypes(); // Pre-materializes all UDTs in the right order
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass ANALYZE in %dms", time);
    }

    private void process(final CompileContext context) {
        final var tokenStream = Objects.requireNonNull(context.getTokenStream());
        final var tokens = tokenStream.getTokens();

        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.PROCESS);
        processTokens(tokens);
        final var time = System.currentTimeMillis() - startTime;

        Logger.INSTANCE.debugln("Finished pass PROCESS in %dms", time);
        final var newTokenStream = new CommonTokenStream(new ListTokenSource(tokens));
        newTokenStream.fill();
        context.setTokenStream(newTokenStream);

        final var parser = Objects.requireNonNull(context.getParser());
        parser.setTokenStream(newTokenStream);
        parser.reset();
        parser.removeErrorListeners();
        context.setFileContext(parser.file());
    }

    private void compile(final CompileContext context) {
        final var startTime = System.currentTimeMillis();
        context.setCurrentPass(CompilePass.COMPILE);
        final var translationUnit = new TranslationUnit(this, context);
        ParseTreeWalker.DEFAULT.walk(translationUnit, context.getFileContext()); // Walk the entire AST with the TU
        context.setTranslationUnit(translationUnit);
        final var time = System.currentTimeMillis() - startTime;
        Logger.INSTANCE.debugln("Finished pass COMPILE in %dms", time);
    }

    private final class ErrorListener implements ANTLRErrorListener {
        private final CompileContext context;

        public ErrorListener(final CompileContext context) {
            this.context = context;
        }

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            context.reportError(context.makeError((Token) offendingSymbol, Utils.capitalize(msg), CompileErrorCode.E2000));
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
    }
}

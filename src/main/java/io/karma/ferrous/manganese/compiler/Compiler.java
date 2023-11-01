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
import io.karma.ferrous.manganese.linker.LinkModel;
import io.karma.ferrous.manganese.linker.Linker;
import io.karma.ferrous.manganese.profiler.Profiler;
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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
    private final Linker linker;
    private final ExecutorService executorService;
    private final AtomicInteger numRunningTasks = new AtomicInteger(0);

    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;

    @API(status = Status.INTERNAL)
    public Compiler(final TargetMachine targetMachine, final Linker linker, final int numThreads) {
        this.targetMachine = targetMachine;
        this.linker = linker;
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
            context.setCurrentPass(CompilePass.TOKENIZE);
            Profiler.INSTANCE.push("Tokenize");
            final var lexer = new FerrousLexer(CharStreams.fromChannel(in, StandardCharsets.UTF_8));
            context.setLexer(lexer);
            final var tokenStream = new CommonTokenStream(lexer);
            tokenStream.fill();
            context.setTokenStream(tokenStream);
            Profiler.INSTANCE.pop();
            if (tokenView) {
                System.out.printf("\n%s\n",
                    TokenUtils.renderTokenTree(context.getCurrentModuleName(),
                        extendedTokenView,
                        lexer,
                        tokenStream.getTokens()));
            }
            // Parse
            context.setCurrentPass(CompilePass.PARSE);
            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(new ErrorListener(context));
            context.setParser(parser);
            Profiler.INSTANCE.push("Parse");
            context.setFileContext(parser.file());
            Profiler.INSTANCE.pop();

            context.setCurrentPass(CompilePass.NONE);
        }
        catch (IOException error) {
            context.setCurrentPass(CompilePass.NONE);
            context.reportError(context.makeError(CompileErrorCode.E0002));
        }
    }

    public void analyzeAndProcess(final String name, final CompileContext context) {
        context.setCurrentModuleName(name);

        context.setCurrentPass(CompilePass.ANALYZE);
        final var analyzer = new Analyzer(this, context);
        context.setAnalyzer(analyzer);
        final var fileContext = Objects.requireNonNull(context.getFileContext());
        Profiler.INSTANCE.push("Analyzer");
        ParseTreeWalker.DEFAULT.walk(analyzer, fileContext);
        analyzer.preProcessTypes(); // Pre-materializes all UDTs in the right order
        Profiler.INSTANCE.pop();

        final var tokenStream = Objects.requireNonNull(context.getTokenStream());
        final var tokens = tokenStream.getTokens();

        context.setCurrentPass(CompilePass.PROCESS);
        Profiler.INSTANCE.push("Process Tokens");
        processTokens(tokens);
        Profiler.INSTANCE.pop();

        final var newTokenStream = new CommonTokenStream(new ListTokenSource(tokens));
        newTokenStream.fill();
        context.setTokenStream(newTokenStream);

        final var parser = Objects.requireNonNull(context.getParser());
        parser.setTokenStream(newTokenStream);
        parser.reset();
        parser.removeErrorListeners();
        context.setFileContext(parser.file());
    }

    public void compile(final String name, final String sourceName, final CompileContext context) {
        context.setCurrentModuleName(name);

        context.setCurrentPass(CompilePass.COMPILE);
        final var translationUnit = new TranslationUnit(this, context);
        Profiler.INSTANCE.push("Translate");
        ParseTreeWalker.DEFAULT.walk(translationUnit, context.getFileContext()); // Walk the entire AST with the TU
        context.setTranslationUnit(translationUnit);
        Profiler.INSTANCE.pop();

        final var module = Objects.requireNonNull(context.getTranslationUnit()).getModule();
        module.setSourceFileName(sourceName);
        final var verificationStatus = module.verify();
        if (verificationStatus != null) {
            context.reportError(context.makeError(CompileErrorCode.E0002));
            return;
        }

        if (disassemble) {
            Logger.INSTANCE.infoln("\n%s", module.disassembleBitcode());
        }

        context.addModule(module);
        context.setCurrentPass(CompilePass.NONE);
    }

    public CompileResult compile(final Path in, final Path out, final CompileContext context) {
        final var outDirectory = out.getParent();
        if (!Files.exists(outDirectory)) {
            try {
                Files.createDirectories(outDirectory);
            }
            catch (Exception error) {
                context.reportError(context.makeError(error.getMessage(), CompileErrorCode.E0005));
                return context.makeResult();
            }
        }

        final var inputFiles = Utils.findFilesWithExtensions(in, IN_EXTENSIONS);
        final var numFiles = inputFiles.size();
        final var maxProgress = (numFiles << 1) + 1;

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            final var rawFileName = Utils.getRawFileName(file);

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

            numRunningTasks.incrementAndGet();
            executorService.submit(() -> {
                context.setCurrentSourceFile(file);
                Logger.INSTANCE.debugln("Input: %s (Thread %d)", file, Thread.currentThread().threadId());
                try (final var stream = Files.newInputStream(file); final var channel = Channels.newChannel(stream)) {
                    tokenizeAndParse(rawFileName, channel, context);
                    analyzeAndProcess(rawFileName, context);
                }
                catch (IOException error) {
                    context.reportError(context.makeError(CompileErrorCode.E0003));
                }
                context.setCurrentSourceFile(null);
                numRunningTasks.decrementAndGet();
            });
        }

        while (numRunningTasks.get() > 0) {
            Thread.yield();
        }

        final var moduleName = Utils.getRawFileName(in);
        final var module = targetMachine.createModule(moduleName);
        module.setSourceFileName(String.format("%s.o", moduleName));

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            context.setCurrentSourceFile(file);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(Utils.getProgressIndicator(maxProgress, numFiles + 1 + i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = Utils.getRawFileName(file);

            compile(rawFileName, file.getFileName().toString(), context);
            context.setCurrentPass(CompilePass.LINK);
            module.linkIn(context.getModule());
            context.setCurrentPass(CompilePass.NONE);

            context.setCurrentSourceFile(null);
        }

        final var globalModule = Objects.requireNonNull(Functions.tryGet(() -> targetMachine.loadEmbeddedModule("global",
            LLVMGetGlobalContext())));
        module.linkIn(globalModule);
        globalModule.dispose();

        if (disassemble) {
            Logger.INSTANCE.infoln("Linked disassembly:\n\n%s", module.disassembleBitcode());
            Logger.INSTANCE.infoln("Native disassembly:\n\n%s", module.disassembleAssembly(targetMachine));
        }

        final var objectFile = out.getParent().resolve(String.format("%s.o", Utils.getRawFileName(out)));
        module.generateAssembly(targetMachine, FileType.OBJECT, objectFile);
        module.dispose();

        // @formatter:off
        Logger.INSTANCE.infoln(Ansi.ansi()
            .fg(Color.GREEN)
            .a(Utils.getProgressIndicator(maxProgress, maxProgress))
            .a(Attribute.RESET)
            .a(" Linking module ")
            .fg(Color.BLUE)
            .a(Attribute.INTENSITY_BOLD)
            .a(out)
            .a(Attribute.RESET)
            .toString());
        // @formatter:on
        linker.link(this,
            context,
            out,
            objectFile,
            LinkModel.FULL,
            targetMachine.getTarget()); // TODO: parse link model from CLI

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

    public Linker getLinker() {
        return linker;
    }

    private void processTokens(final List<Token> tokens) {
        // TODO: implement here
    }

    private final class ErrorListener implements ANTLRErrorListener {
        private final CompileContext context;

        public ErrorListener(final CompileContext context) {
            this.context = context;
        }

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            context.reportError(context.makeError((Token) offendingSymbol,
                Utils.capitalize(msg),
                CompileErrorCode.E2000));
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
                Logger.INSTANCE.debugln("Detected abnormally high context sensitivity at %d:%d (%d)",
                    startIndex,
                    stopIndex,
                    dfa.decision);
            }
        }
    }
}

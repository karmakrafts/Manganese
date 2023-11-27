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

import io.karma.ferrous.manganese.compiler.pass.*;
import io.karma.ferrous.manganese.linker.LinkModel;
import io.karma.ferrous.manganese.linker.LinkTargetType;
import io.karma.ferrous.manganese.linker.Linker;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.target.FileType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.function.Functions;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private final ArrayList<CompilePass> passes = new ArrayList<>();

    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private boolean disassemble = false;

    @API(status = Status.INTERNAL)
    public Compiler(final TargetMachine targetMachine, final Linker linker, final int numThreads) {
        this.targetMachine = targetMachine;
        this.linker = linker;
        executorService = Executors.newWorkStealingPool(numThreads);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> Functions.tryDo(() -> {
            executorService.shutdown();
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow().forEach(Runnable::run);
            }
        })));
        addDefaultPasses();
    }

    public ArrayList<CompilePass> getPasses() {
        return new ArrayList<>(passes); // Always copy
    }

    @SuppressWarnings("unchecked")
    public <P extends CompilePass> P getPass(final Class<P> type) {
        for (final var pass : passes) {
            if (!pass.getClass().equals(type)) {
                continue;
            }
            return (P) pass;
        }
        throw new IllegalStateException("No such compile pass");
    }

    public boolean addPass(final CompilePass pass) {
        if (passes.contains(pass)) {
            return false;
        }
        passes.add(pass);
        return true;
    }

    public boolean addPassBefore(final Class<? extends CompilePass> beforeType, final CompilePass pass) {
        for (final var toCompare : passes) {
            if (!toCompare.getClass().equals(beforeType)) {
                continue;
            }
            passes.add(Math.max(0, passes.indexOf(toCompare) - 1), pass);
            return true;
        }
        return false;
    }

    public boolean addPassAfter(final Class<? extends CompilePass> afterType, final CompilePass pass) {
        for (final var toCompare : passes) {
            if (!toCompare.getClass().equals(afterType)) {
                continue;
            }
            passes.add(passes.indexOf(toCompare), pass);
            return true;
        }
        return false;
    }

    private void addDefaultPasses() {
        passes.add(new TypeDiscoveryPass());
        passes.add(new TypeResolutionPass());
        passes.add(new FunctionDeclarationPass());
        passes.add(new FunctionDefinitionPass());
        passes.add(new EmitPass());
    }

    private void tokenizeAndParse(final String name, final ReadableByteChannel in, final CompileContext context) {
        try {
            context.setCurrentModuleName(name);
            final var moduleData = context.getOrCreateModuleData();
            // Tokenize
            Profiler.INSTANCE.push("Tokenize");
            final var lexer = new FerrousLexer(CharStreams.fromChannel(in, StandardCharsets.UTF_8));
            moduleData.setLexer(lexer);
            final var tokenStream = new CommonTokenStream(lexer);
            tokenStream.fill();
            moduleData.setTokenStream(tokenStream);
            Profiler.INSTANCE.pop();
            if (tokenView) {
                System.out.printf("\n%s\n",
                    TokenUtils.renderTokenTree(context.getCurrentModuleName(),
                        extendedTokenView,
                        lexer,
                        tokenStream.getTokens()));
            }
            // Parse
            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(new ErrorListener(context));
            moduleData.setParser(parser);
            Profiler.INSTANCE.push("Parse");
            moduleData.setFileContext(parser.file());
            Profiler.INSTANCE.pop();
        }
        catch (IOException error) {
            context.reportError(CompileErrorCode.E0002);
        }
    }

    public Module compile(final String name, final @Nullable Path sourcePath, final CompileContext context) {
        context.setCurrentModuleName(name);
        context.setCurrentSourceFile(sourcePath);
        final var module = targetMachine.createModule(name);
        for (final var pass : passes) {
            Logger.INSTANCE.debugln("Invoking pass %s", pass.getClass().getName());
            context.setCurrentPass(pass);
            pass.run(this, context, module, executorService);
            context.setCurrentPass(null);
        }
        return module;
    }

    public CompileResult compile(final Path in, final Path out, final CompileContext context, final LinkModel linkModel,
                                 final LinkTargetType targetType) {
        final var outDirectory = out.getParent();
        if (!Files.exists(outDirectory)) {
            try {
                Files.createDirectories(outDirectory);
            }
            catch (Exception error) {
                context.reportError(error.getMessage(), CompileErrorCode.E0005);
                return context.makeResult();
            }
        }

        final var inputFiles = KitchenSink.findFilesWithExtensions(in, IN_EXTENSIONS);
        final var numFiles = inputFiles.size();
        final var maxProgress = (numFiles << 1) + 1;
        final var futures = new ArrayDeque<CompletableFuture<Void>>();

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            final var rawFileName = KitchenSink.getRawFileName(file);

            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(KitchenSink.getProgressIndicator(maxProgress, i))
                .a(Attribute.RESET)
                .a(" Analyzing file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on

            futures.add(CompletableFuture.runAsync(() -> {
                context.setCurrentSourceFile(file);
                Logger.INSTANCE.debugln("Input: %s (Thread %d)", file, Thread.currentThread().threadId());
                try (final var stream = Files.newInputStream(file); final var channel = Channels.newChannel(stream)) {
                    tokenizeAndParse(rawFileName, channel, context);
                }
                catch (IOException error) {
                    context.reportError(CompileErrorCode.E0003);
                }
                context.setCurrentSourceFile(null);
            }, executorService));
        }

        for (final var future : futures) {
            while (!future.isDone()) {
                Thread.onSpinWait();
            }
        }

        final var moduleName = KitchenSink.getRawFileName(in);
        final var projectModule = targetMachine.createModule(moduleName);
        projectModule.setSourceFileName(String.format("%s.o", moduleName));

        for (var i = 0; i < numFiles; ++i) {
            final var file = inputFiles.get(i);
            context.setCurrentSourceFile(file);
            // @formatter:off
            Logger.INSTANCE.infoln(Ansi.ansi()
                .fg(Color.GREEN)
                .a(KitchenSink.getProgressIndicator(maxProgress, numFiles + 1 + i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(file.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on
            final var rawFileName = KitchenSink.getRawFileName(file);

            projectModule.linkIn(compile(rawFileName, file, context));
            context.setCurrentSourceFile(null);
        }

        if (disassemble) {
            Logger.INSTANCE.infoln("Linked disassembly:\n\n%s", projectModule.disassembleBitcode());
            Logger.INSTANCE.infoln("Native disassembly:\n\n%s", projectModule.disassembleAssembly(targetMachine));
        }

        final var objectFile = out.getParent().resolve(String.format("%s.o", KitchenSink.getRawFileName(out)));
        projectModule.generateAssembly(targetMachine, FileType.OBJECT, objectFile);
        projectModule.dispose();

        // @formatter:off
        Logger.INSTANCE.infoln(Ansi.ansi()
            .fg(Color.GREEN)
            .a(KitchenSink.getProgressIndicator(maxProgress, maxProgress))
            .a(Attribute.RESET)
            .a(" Linking module ")
            .fg(Color.BLUE)
            .a(Attribute.INTENSITY_BOLD)
            .a(out)
            .a(Attribute.RESET)
            .toString());
        // @formatter:on
        linker.link(this, context, out, objectFile, linkModel, targetMachine, targetType);

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

    private final class ErrorListener implements ANTLRErrorListener {
        private final CompileContext context;

        public ErrorListener(final CompileContext context) {
            this.context = context;
        }

        @Override
        public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                                final int charPositionInLine, final String msg, final RecognitionException e) {
            context.reportError((Token) offendingSymbol,
                KitchenSink.makeCompilerMessage(KitchenSink.capitalize(msg)),
                CompileErrorCode.E2000);
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

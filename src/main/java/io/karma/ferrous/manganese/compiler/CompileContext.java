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
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.module.ModuleData;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class CompileContext {
    private final ArrayList<CompileError> errors = new ArrayList<>();
    private final ReentrantLock errorsMutex = new ReentrantLock();
    private final HashMap<String, Module> modules = new HashMap<>();
    private final ReentrantLock modulesMutex = new ReentrantLock();
    private final HashMap<String, ModuleData> moduleData = new HashMap<>();
    private final ReentrantLock moduleDataMutex = new ReentrantLock();
    private final ThreadLocal<ThreadLocals> threadLocals = ThreadLocal.withInitial(ThreadLocals::new);

    public Module getModule() {
        try {
            modulesMutex.lock();
            return Objects.requireNonNull(modules.get(Objects.requireNonNull(getCurrentModuleName())));
        } finally {
            modulesMutex.unlock();
        }
    }
    // @formatter:on

    private ModuleData getOrCreateModuleData(final String name) {
        try {
            moduleDataMutex.lock();
            return moduleData.computeIfAbsent(name, ModuleData::new);
        } finally {
            moduleDataMutex.unlock();
        }
    }

    private ModuleData getOrCreateModuleData() {
        return getOrCreateModuleData(Objects.requireNonNull(getCurrentModuleName()));
    }

    private <T> T getModuleComponent(final Function<ModuleData, T> selector) {
        return selector.apply(getOrCreateModuleData());
    }

    public CompileResult makeResult() {
        return new CompileResult(getCurrentStatus(), new ArrayList<>(errors));
    }

    public CompileError makeError(final CompileErrorCode errorCode) {
        return new CompileError(null, null, getCurrentPass(), null, errorCode);
    }

    public CompileError makeError(final String text, final CompileErrorCode errorCode) {
        return new CompileError(null, null, getCurrentPass(), text, errorCode);
    }

    public CompileError makeError(final Token token, final CompileErrorCode errorCode) {
        return new CompileError(token, TokenUtils.getLineTokens(getTokenStream(), token), getCurrentPass(), null,
                errorCode);
    }

    public CompileError makeError(final Token token, final String text, final CompileErrorCode errorCode) {
        return new CompileError(token, TokenUtils.getLineTokens(getTokenStream(), token), getCurrentPass(), text,
                errorCode);
    }

    public void reportError(final CompileError error) {
        final var tokenStream = getTokenStream();
        if (tokenStream != null && tokenStream.size() == 0) {
            tokenStream.fill();
        }
        try {
            errorsMutex.lock();
            if (errors.contains(error)) {
                return; // Don't report duplicates
            }
            errors.add(error);
        } finally {
            errorsMutex.unlock();
        }
        setCurrentStatus(getCurrentStatus().worse(error.getStatus()));
    }

    public CompilePass getCurrentPass() {
        return threadLocals.get().currentPass;
    }

    public void setCurrentPass(final CompilePass currentPass) {
        threadLocals.get().currentPass = currentPass;
    }

    public CompileStatus getCurrentStatus() {
        return threadLocals.get().currentStatus;
    }

    public void setCurrentStatus(final CompileStatus status) {
        threadLocals.get().currentStatus = status;
    }

    public @Nullable String getCurrentModuleName() {
        return threadLocals.get().currentModuleName;
    }

    void setModuleName(final @Nullable String currentName) {
        threadLocals.get().currentModuleName = currentName;
    }

    public FerrousLexer getLexer() {
        return getModuleComponent(ModuleData::getLexer);
    }

    void setLexer(final FerrousLexer lexer) {
        getOrCreateModuleData().setLexer(lexer);
    }

    public BufferedTokenStream getTokenStream() {
        return getModuleComponent(ModuleData::getTokenStream);
    }

    void setTokenStream(final BufferedTokenStream tokenStream) {
        getOrCreateModuleData().setTokenStream(tokenStream);
    }

    public FerrousParser getParser() {
        return getModuleComponent(ModuleData::getParser);
    }

    void setParser(final FerrousParser parser) {
        getOrCreateModuleData().setParser(parser);
    }

    public FileContext getFileContext() {
        return getModuleComponent(ModuleData::getFileContext);
    }

    void setFileContext(final FileContext fileContext) {
        getOrCreateModuleData().setFileContext(fileContext);
    }

    public Analyzer getAnalyzer() {
        return getModuleComponent(ModuleData::getAnalyzer);
    }

    void setAnalyzer(final Analyzer analyzer) {
        getOrCreateModuleData().setAnalyzer(analyzer);
    }

    public TranslationUnit getTranslationUnit() {
        return getModuleComponent(ModuleData::getTranslationUnit);
    }

    void setTranslationUnit(final TranslationUnit translationUnit) {
        getOrCreateModuleData().setTranslationUnit(translationUnit);
    }

    public void dispose() {
        try {
            modulesMutex.lock();
            modules.values().forEach(Module::dispose); // Dispose the actual modules
            modules.clear();
        } finally {
            modulesMutex.unlock();
        }
        try {
            moduleDataMutex.lock();
            moduleData.clear();
        } finally {
            moduleDataMutex.unlock();
        }
    }

    public void addModule(final Module module) {
        try {
            modulesMutex.lock();
            modules.put(module.getName(), module);
        } finally {
            modulesMutex.unlock();
        }
    }

    // @formatter:off
    private static final class ThreadLocals {
        public CompilePass currentPass = CompilePass.NONE;
        public CompileStatus currentStatus = CompileStatus.SKIPPED;
        public String currentModuleName;
    }
}

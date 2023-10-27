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

import java.util.*;
import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class CompileContext {
    private final List<CompileError> errors = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Module> modules = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, ModuleData> moduleData = Collections.synchronizedMap(new HashMap<>());
    private final ThreadLocal<CompileStatus> status = ThreadLocal.withInitial(() -> CompileStatus.SUCCESS);
    private final ThreadLocal<CompilePass> pass = ThreadLocal.withInitial(() -> CompilePass.NONE);
    private final ThreadLocal<String> currentModuleName = ThreadLocal.withInitial(String::new);

    public synchronized Module getModule() {
        return Objects.requireNonNull(modules.get(Objects.requireNonNull(getCurrentModuleName())));
    }
    // @formatter:on

    private synchronized ModuleData getOrCreateModuleData(final String name) {
        return moduleData.computeIfAbsent(name, ModuleData::new);
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
        synchronized (this) {
            if (errors.contains(error)) {
                return; // Don't report duplicates
            }
            errors.add(error);
        }
        setCurrentStatus(getCurrentStatus().worse(error.getStatus()));
    }

    public CompilePass getCurrentPass() {
        return pass.get();
    }

    public void setCurrentPass(final CompilePass currentPass) {
        pass.set(currentPass);
    }

    public CompileStatus getCurrentStatus() {
        return status.get();
    }

    public void setCurrentStatus(final CompileStatus status) {
        this.status.set(status);
    }

    public @Nullable String getCurrentModuleName() {
        return currentModuleName.get();
    }

    void setCurrentModuleName(final @Nullable String currentName) {
        currentModuleName.set(currentName);
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

    public synchronized void dispose() {
        modules.values().forEach(Module::dispose); // Dispose the actual modules
        modules.clear();
        moduleData.clear();
    }

    public synchronized void addModule(final Module module) {
        modules.put(module.getName(), module);
    }
}

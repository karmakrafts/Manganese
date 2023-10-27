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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class CompileContext {
    private final ArrayList<CompileError> errors = new ArrayList<>();
    private final HashMap<String, Module> modules = new HashMap<>();
    private final HashMap<String, ModuleData> moduleData = new HashMap<>();

    private CompilePass currentPass = CompilePass.NONE;
    private CompileStatus currentStatus = CompileStatus.SKIPPED;
    private String currentModuleName;

    public Module getModule() {
        return Objects.requireNonNull(modules.get(Objects.requireNonNull(currentModuleName)));
    }

    private ModuleData getOrCreateModuleData(final String name) {
        var result = moduleData.get(name);
        if (result == null) {
            result = new ModuleData(name);
            moduleData.put(name, result);
        }
        return result;
    }

    private ModuleData getOrCreateModuleData() {
        return getOrCreateModuleData(Objects.requireNonNull(currentModuleName));
    }

    private <T> T getModuleComponent(final Function<ModuleData, T> selector) {
        final var data = moduleData.get(Objects.requireNonNull(currentModuleName));
        if (data != null) {
            return selector.apply(data);
        }
        throw new IllegalStateException("No such module component");
    }

    public CompileResult makeResult() {
        return new CompileResult(currentStatus, new ArrayList<>(errors));
    }

    public CompileError makeError(final CompileErrorCode errorCode) {
        return new CompileError(null, null, currentPass, null, errorCode);
    }

    public CompileError makeError(final String text, final CompileErrorCode errorCode) {
        return new CompileError(null, null, currentPass, text, errorCode);
    }

    public CompileError makeError(final Token token, final CompileErrorCode errorCode) {
        return new CompileError(token, TokenUtils.getLineTokens(getTokenStream(), token), currentPass, null, errorCode);
    }

    public CompileError makeError(final Token token, final String text, final CompileErrorCode errorCode) {
        return new CompileError(token, TokenUtils.getLineTokens(getTokenStream(), token), currentPass, text, errorCode);
    }

    public void reportError(final CompileError error) {
        final var tokenStream = getTokenStream();
        if (tokenStream != null && tokenStream.size() == 0) {
            tokenStream.fill();
        }
        if (errors.contains(error)) {
            return; // Don't report duplicates
        }
        errors.add(error);
        this.currentStatus = this.currentStatus.worse(error.getStatus());
    }

    public CompilePass getCurrentPass() {
        return currentPass;
    }

    public void setCurrentPass(final CompilePass currentPass) {
        this.currentPass = currentPass;
    }

    public CompileStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(final CompileStatus status) {
        this.currentStatus = status;
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

    public String getModuleName() {
        return currentModuleName;
    }

    void setModuleName(final String currentName) {
        currentModuleName = currentName;
    }

    public void dispose() {
        modules.values().forEach(Module::dispose); // Dispose the actual modules
        modules.clear();
        moduleData.clear();
    }

    public void addModule(final Module module) {
        modules.put(currentModuleName, module);
    }
}

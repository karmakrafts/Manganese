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
    private CompileStatus status = CompileStatus.SKIPPED;
    private String moduleName;

    public Module getModule() {
        return Objects.requireNonNull(modules.get(Objects.requireNonNull(moduleName)));
    }

    private ModuleData getOrCreateModuleData(final String name) {
        var result = moduleData.get(name);
        if (result == null) {
            result = new ModuleData(name);
            moduleData.put(name, result);
        }
        return result;
    }

    public CompileResult makeResult() {
        return new CompileResult(status, new ArrayList<>(errors));
    }

    public CompileError makeError(final Token token) {
        return new CompileError(token, getTokenStream());
    }

    public CompileError makeError(final Token token, final String additionalText) {
        final var error = new CompileError(token, getTokenStream());
        error.setAdditionalText(additionalText);
        return error;
    }

    public void reportError(final CompileError error, final CompileStatus status) {
        final var tokenStream = getTokenStream();
        if (tokenStream != null && tokenStream.size() == 0) {
            tokenStream.fill();
        }
        if (errors.contains(error)) {
            return; // Don't report duplicates
        }
        errors.add(error);
        this.status = this.status.worse(status);
    }

    public CompilePass getCurrentPass() {
        return currentPass;
    }

    public void setCurrentPass(CompilePass currentPass) {
        this.currentPass = currentPass;
    }

    public CompileStatus getStatus() {
        return status;
    }

    public void setStatus(CompileStatus status) {
        this.status = status;
    }

    public @Nullable FerrousLexer getLexer() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getLexer();
        }
        return null;
    }

    void setLexer(final FerrousLexer lexer) {
        if (moduleName == null) {
            throw new IllegalStateException("Module name not provided");
        }
        getOrCreateModuleData(moduleName).setLexer(lexer);
    }

    public @Nullable BufferedTokenStream getTokenStream() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getTokenStream();
        }
        return null;
    }

    void setTokenStream(final BufferedTokenStream tokenStream) {
        if (moduleName == null) {
            throw new IllegalStateException("Module name not provided");
        }
        getOrCreateModuleData(moduleName).setTokenStream(tokenStream);
    }

    public @Nullable FerrousParser getParser() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getParser();
        }
        return null;
    }

    void setParser(final FerrousParser parser) {
        if (moduleName == null) {
            throw new IllegalStateException("Module name not provided");
        }
        getOrCreateModuleData(moduleName).setParser(parser);
    }

    public @Nullable FileContext getFileContext() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getFileContext();
        }
        return null;
    }

    void setFileContext(final FileContext fileContext) {
        if (moduleName == null) {
            throw new IllegalStateException("Module name not provided");
        }
        getOrCreateModuleData(moduleName).setFileContext(fileContext);
    }

    public @Nullable Analyzer getAnalyzer() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getAnalyzer();
        }
        return null;
    }

    void setAnalyzer(final Analyzer analyzer) {
        if (moduleName == null) {
            throw new IllegalStateException("Analyzer not provided");
        }
        getOrCreateModuleData(moduleName).setAnalyzer(analyzer);
    }

    public @Nullable TranslationUnit getTranslationUnit() {
        final var moduleData = this.moduleData.get(moduleName);
        if (moduleData != null) {
            return moduleData.getTranslationUnit();
        }
        return null;
    }

    void setTranslationUnit(final TranslationUnit translationUnit) {
        if (moduleName == null) {
            throw new IllegalStateException("Analyzer not provided");
        }
        getOrCreateModuleData(moduleName).setTranslationUnit(translationUnit);
    }

    public String getModuleName() {
        return moduleName;
    }

    void setModuleName(final String currentName) {
        moduleName = currentName;
    }

    public ArrayList<CompileError> getErrors() {
        return errors;
    }

    public HashMap<String, Module> getModules() {
        return modules;
    }

    public HashMap<String, ModuleData> getModuleData() {
        return moduleData;
    }

    public void dispose() {
        modules.values().forEach(Module::dispose); // Dispose the actual modules
        modules.clear();
        moduleData.clear();
    }

    public void addModule(final Module module) {
        modules.put(moduleName, module);
    }
}

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

import io.karma.ferrous.manganese.Module;
import io.karma.ferrous.manganese.analyze.Analyzer;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
public final class CompileContext {
    private final ArrayList<CompileError> errors = new ArrayList<>();
    private final HashMap<String, Module> modules = new HashMap<>();
    private CompilePass currentPass = CompilePass.NONE;
    private CompileStatus status = CompileStatus.SKIPPED;
    private FerrousLexer lexer;
    private BufferedTokenStream tokenStream;
    private FerrousParser parser;
    private FileContext fileContext;
    private Analyzer analyzer;
    private TranslationUnit translationUnit;
    private String moduleName;

    public CompileResult makeResult(final List<Path> compiledFiles) {
        return new CompileResult(status, compiledFiles, new ArrayList<>(errors));
    }

    public CompileResult makeResult(final Path... compiledFiles) {
        return makeResult(Arrays.asList(compiledFiles));
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

    public FerrousLexer getLexer() {
        return lexer;
    }

    void setLexer(final FerrousLexer lexer) {
        this.lexer = lexer;
    }

    public BufferedTokenStream getTokenStream() {
        return tokenStream;
    }

    void setTokenStream(final BufferedTokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }

    public FerrousParser getParser() {
        return parser;
    }

    void setParser(final FerrousParser parser) {
        this.parser = parser;
    }

    public FileContext getFileContext() {
        return fileContext;
    }

    void setFileContext(final FileContext fileContext) {
        this.fileContext = fileContext;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    void setAnalyzer(final Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public TranslationUnit getTranslationUnit() {
        return translationUnit;
    }

    void setTranslationUnit(final TranslationUnit translationUnit) {
        this.translationUnit = translationUnit;
    }

    public String getModuleName() {
        return moduleName;
    }

    void setModuleName(final String currentName) {
        this.moduleName = currentName;
    }

    public ArrayList<CompileError> getErrors() {
        return errors;
    }

    public HashMap<String, Module> getModules() {
        return modules;
    }

    public void dispose() {
        if (translationUnit != null) {
            translationUnit.dispose();
        }
        modules.values().forEach(Module::dispose);
        modules.clear();
    }
}

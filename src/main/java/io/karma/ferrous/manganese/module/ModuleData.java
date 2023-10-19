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

package io.karma.ferrous.manganese.module;

import io.karma.ferrous.manganese.analyze.Analyzer;
import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class ModuleData {
    private final String name;
    private BufferedTokenStream tokenStream;
    private Analyzer analyzer;
    private TranslationUnit translationUnit;
    private FileContext fileContext;
    private FerrousLexer lexer;
    private FerrousParser parser;

    public ModuleData(final String name) {
        this.name = name;
    }

    public FerrousLexer getLexer() {
        return lexer;
    }

    @API(status = Status.INTERNAL)
    public void setLexer(final FerrousLexer lexer) {
        this.lexer = lexer;
    }

    public FerrousParser getParser() {
        return parser;
    }

    @API(status = Status.INTERNAL)
    public void setParser(final FerrousParser parser) {
        this.parser = parser;
    }

    public FileContext getFileContext() {
        return fileContext;
    }

    @API(status = Status.INTERNAL)
    public void setFileContext(final FileContext fileContext) {
        this.fileContext = fileContext;
    }

    public BufferedTokenStream getTokenStream() {
        return tokenStream;
    }

    @API(status = Status.INTERNAL)
    public void setTokenStream(final BufferedTokenStream tokenStream) {
        this.tokenStream = tokenStream;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    @API(status = Status.INTERNAL)
    public void setAnalyzer(final Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public TranslationUnit getTranslationUnit() {
        return translationUnit;
    }

    @API(status = Status.INTERNAL)
    public void setTranslationUnit(final TranslationUnit translationUnit) {
        this.translationUnit = translationUnit;
    }

    public String getName() {
        return name;
    }

    public void dispose() {
        translationUnit.dispose();
    }
}
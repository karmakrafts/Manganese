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

import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public final class CompileError implements Comparable<CompileError> {
    private final Token token;
    private final List<Token> lineTokens;
    private final String ansiText;
    private final String text;
    private final int line;
    private final int column;
    private String additionalText;

    public CompileError(final Token token, final List<Token> lineTokens, final int line, final int column) {
        this.token = token;
        this.lineTokens = lineTokens;
        this.line = line;
        this.column = column;

        final var ansiBuffer = Ansi.ansi();
        final var buffer = new StringBuilder();
        for (final var lineToken : lineTokens) {
            if (lineToken.getText().equals("\n")) {
                continue; // Skip any new lines
            }
            handleTokenColor(lineToken, ansiBuffer);
            final var tokenText = lineToken.getText();
            ansiBuffer.a(tokenText);
            buffer.append(tokenText);
        }
        ansiText = ansiBuffer.toString().strip();
        text = buffer.toString().strip();
    }

    public CompileError(final Token token, final TokenStream tokenStream, final int line, final int column) {
        this(token, TokenUtils.getLineTokens(tokenStream, token), line, column);
    }

    public CompileError(final Token token, final int line, final int column) {
        this(token, Compiler.getInstance().getTokenStream(), line, column);
    }

    public CompileError(final Token token) {
        this(token, token.getLine(), token.getCharPositionInLine());
    }

    private static void handleTokenColor(final Token token, final Ansi buffer) {
        final var tokenType = token.getType();
        // @formatter:off
        if (tokenType >= FerrousLexer.KW_STACKALLOC && tokenType <= FerrousLexer.KW_F64) {
            buffer.fgBright(Color.MAGENTA);
        }
        else if (tokenType >= FerrousLexer.LITERAL_I8 && tokenType <= FerrousLexer.LITERAL_F64) {
            buffer.fgBright(Color.BLUE);
        }
        else if ((tokenType >= FerrousLexer.ML_STRING_END && tokenType <= FerrousLexer.ML_STRING_BEGIN)
                 || (tokenType >= FerrousLexer.STRING_MODE_ESCAPED_STRING_END && tokenType <= FerrousLexer.ML_STRING_MODE_TEXT)) {
            buffer.fgBright(Color.GREEN);
        }
        else if (tokenType == FerrousLexer.IDENT) {
            buffer.fgBright(Color.YELLOW);
        }
        else {
            buffer.a(Attribute.RESET);
        }
        // @formatter:on
    }

    public Optional<String> getAdditionalText() {
        return Optional.ofNullable(additionalText);
    }

    public void setAdditionalText(final String additionalText) {
        this.additionalText = additionalText;
    }

    @Override
    public int compareTo(final @NotNull CompileError other) {
        final var line = Integer.compare(this.line, other.line);
        if (line != 0) {
            return line;
        }
        return Integer.compare(other.column, this.column);
    }

    public String getAnsiText() {
        return ansiText;
    }

    public String getText() {
        return text;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Token getToken() {
        return token;
    }

    public List<Token> getLineTokens() {
        return lineTokens;
    }

    public void print(final PrintStream stream) {
        // @formatter:off
        stream.printf(Ansi.ansi().fg(Color.RED).a("\nUnexpected symbol in line %d:%d\n\n")
            .a(Attribute.RESET).toString(), line, column);
        // @formatter:on
        stream.printf("  %s\n", ansiText);

        stream.print("  ");
        for (var i = 0; i < column; i++) {
            stream.print(' ');
        }
        final var length = token.getStopIndex() - token.getStartIndex() + 1;
        for (var i = 0; i < length; i++) {
            stream.print(Ansi.ansi().fgBright(Color.RED).a('^').a(Attribute.RESET));
        }

        stream.print("\n");
        if (additionalText != null) {
            stream.printf("%s\n", additionalText);
        }
        stream.print("\n");
    }
}

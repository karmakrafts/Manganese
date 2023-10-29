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

import io.karma.ferrous.vanadium.FerrousLexer;
import org.antlr.v4.runtime.Token;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
@API(status = Status.STABLE)
public record CompileError(@Nullable Token token, @Nullable List<Token> lineTokens, @Nullable CompilePass pass,
                           @Nullable String text, @Nullable Path sourceFile, CompileErrorCode errorCode)
    implements Comparable<CompileError> {
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

    @Override
    public int compareTo(final @NotNull CompileError other) {
        if (token == null) {
            return -1; // Errors with no lines go to the back
        }
        if (other.token == null) {
            return 1; // If the other token is null and we are not, step over
        }
        final var line = Integer.compare(token.getLine(), other.token.getLine());
        if (line != 0) {
            return line;
        }
        return Integer.compare(token.getCharPositionInLine(), other.token.getCharPositionInLine());
    }

    public String getAnsiText() {
        final var ansiBuffer = Ansi.ansi();
        if (lineTokens == null || lineTokens.isEmpty()) {
            if (token != null) {
                handleTokenColor(token, ansiBuffer);
                return ansiBuffer.a(token.getText()).a(Attribute.RESET).toString();
            }
            return "";
        }
        for (final var lineToken : lineTokens) {
            if (lineToken.getText().equals("\n")) {
                continue; // Skip any new lines
            }
            handleTokenColor(lineToken, ansiBuffer);
            ansiBuffer.a(lineToken.getText());
        }
        return ansiBuffer.a(Attribute.RESET).toString().strip();
    }

    public String getText() {
        if (lineTokens == null || lineTokens.isEmpty()) {
            if (token != null) {
                return token.getText();
            }
            return "";
        }
        final var buffer = new StringBuilder();
        for (final var lineToken : lineTokens) {
            if (lineToken.getText().equals("\n")) {
                continue; // Skip any new lines
            }
            buffer.append(lineToken.getText());
        }
        return buffer.toString();
    }

    public int getLine() {
        return token == null ? -1 : token.getLine();
    }

    public int getColumn() {
        return token == null ? -1 : token.getCharPositionInLine();
    }

    public @Nullable CompileStatus getStatus() {
        return errorCode.getStatus();
    }

    public @Nullable CompilePass getPass() {
        return pass;
    }

    public @Nullable Token getToken() {
        return token;
    }

    public @Nullable List<Token> getLineTokens() {
        return lineTokens;
    }

    public CompileErrorCode getErrorCode() {
        return errorCode;
    }

    public void print(final PrintStream stream) {
        stream.print(render());
    }

    public String render() {
        final var builder = Ansi.ansi();

        if(token != null) { // @formatter:off
            final var line = getLine();
            final var column = getColumn();
            builder.a('\n');
            builder.fg(Color.RED);
            if(sourceFile != null) {
                builder.a(String.format("Error during compilation in %s:%d:%d", sourceFile.toAbsolutePath().normalize(), line, column));
            }
            else {
                builder.a(String.format("Error during compilation in %d:%d", line, column));
            }
            builder.a(Attribute.RESET);
            builder.a("\n\n  ");
            builder.a(getAnsiText());
            builder.a("\n  ");
            final var length = Math.max(1, token.getStopIndex() - token.getStartIndex() + 1);
            builder.a(" ".repeat(Math.max(0, column)));
            for (var i = 0; i < length; i++) {
                builder.fgBright(Color.RED);
                builder.a('^');
            }
            builder.a(Attribute.RESET);
            builder.a(' ');
        }// @formatter:on
        else {// @formatter:off
            builder.a('\n');
            builder.fg(Color.RED);
            builder.a("Error during compilation");
            builder.a(Attribute.RESET);
            builder.a("\n\n");
        }// @formatter:on

        if (text != null) {
            builder.fgBright(Color.RED).a(errorCode).a("\n").a(Attribute.RESET);
            builder.a("    ").a(" ".repeat(Math.max(0, getColumn()))).a(text).a("\n\n");
        }
        else {
            builder.fgBright(Color.RED).a(errorCode).a("\n\n").a(Attribute.RESET);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return getText();
    }
}

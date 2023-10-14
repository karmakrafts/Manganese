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

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.vanadium.FerrousLexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public final class TokenUtils {
    // @formatter:off
    private TokenUtils() {}
    // @formatter:on

    public static String getLiteral(final int token) {
        final var name = FerrousLexer.VOCABULARY.getLiteralName(token);
        return name.substring(1, name.length() - 1);
    }

    public static void getLineTokens(final TokenStream tokenStream, final Token token, final List<Token> tokens) {
        final var line = token.getLine();

        var startIndex = token.getTokenIndex() - 1;
        while (startIndex > 0 && tokenStream.get(startIndex).getLine() == line) {
            --startIndex;
        }
        if (startIndex < 0) {
            startIndex = 0;
        }

        final var maxIndex = tokenStream.size() - 1;
        var endIndex = token.getTokenIndex() + 1;
        while (endIndex < maxIndex && tokenStream.get(endIndex).getLine() == line) {
            ++endIndex;
        }
        if (endIndex >= maxIndex) {
            endIndex = maxIndex;
        }

        for (var i = startIndex; i < endIndex; i++) {
            tokens.add(tokenStream.get(i));
        }
    }

    public static ArrayList<Token> getLineTokens(final TokenStream tokenStream, final Token token) {
        final var lineTokens = new ArrayList<Token>();
        getLineTokens(tokenStream, token, lineTokens);
        return lineTokens;
    }
}

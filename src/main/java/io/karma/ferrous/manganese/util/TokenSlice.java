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

import io.karma.ferrous.manganese.compiler.CompileContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 29/10/2023
 */
public record TokenSlice(TokenStream tokenStream, int begin, int end) {
    public static TokenSlice from(final CompileContext compileContext, final ParserRuleContext context) {
        return new TokenSlice(compileContext.getTokenStream(),
            context.start.getTokenIndex(),
            context.stop.getTokenIndex());
    }

    public Token findTokenOrFirst(final String text) {
        for (var i = begin; i <= end; i++) {
            final var token = tokenStream.get(i);
            if (!token.getText().contains(text)) {
                continue;
            }
            return token;
        }
        return getFirstToken();
    }

    public Token getFirstToken() {
        return tokenStream.get(begin);
    }

    public ArrayList<Token> getTokens() {
        final ArrayList<Token> tokens = new ArrayList<>(end - begin);
        for (var i = begin; i <= end; i++) {
            tokens.add(tokenStream.get(i));
        }
        return tokens;
    }
}

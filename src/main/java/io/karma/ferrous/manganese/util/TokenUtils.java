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
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.barfuin.texttree.api.DefaultNode;
import org.barfuin.texttree.api.TextTree;
import org.barfuin.texttree.api.TreeOptions;
import org.barfuin.texttree.api.style.TreeStyles;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
@API(status = Status.INTERNAL)
public final class TokenUtils {
    // @formatter:off
    private TokenUtils() {}
    // @formatter:on

    public static String renderTokenTree(final String name, final boolean printHiddenTokens, final FerrousLexer lexer,
                                         final Collection<Token> tokens) {
        final var nodeStack = new Stack<DefaultNode>();
        nodeStack.push(new DefaultNode(name));
        for (final var token : tokens) {
            switch (token.getType()) {
                case FerrousLexer.R_BRACE:
                case FerrousLexer.R_BRACKET:
                case FerrousLexer.R_PAREN:
                    final var lastNode = nodeStack.pop();
                    nodeStack.peek().addChild(lastNode);
                    break;
            }
            final var text = token.getText();
            if (!printHiddenTokens && text.isBlank()) {
                continue; // Skip blank tokens if extended mode is disabled
            }
            // @formatter:off
            final var tokenTypeEntry = lexer.getTokenTypeMap()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().equals(token.getType()))
                .findFirst()
                .orElseThrow();
            final var formattedText = Ansi.ansi()
                .fg(Color.BLUE)
                .a(String.format("%06d", token.getTokenIndex()))
                .a(Attribute.RESET)
                .a(": ")
                .fgBright(Color.CYAN)
                .a(text.equals("\n") ? " " : text)
                .a(Attribute.RESET)
                .a(' ')
                .fg(Color.GREEN)
                .a(tokenTypeEntry)
                .a(Attribute.RESET).toString();
            // @formatter:on
            switch (token.getType()) {
                case FerrousLexer.L_BRACE:
                case FerrousLexer.L_BRACKET:
                case FerrousLexer.L_PAREN:
                    nodeStack.push(new DefaultNode(formattedText));
                    break;
                default:
                    nodeStack.peek().addChild(new DefaultNode(formattedText));
                    break;
            }
        }
        final var options = new TreeOptions();
        options.setStyle(TreeStyles.UNICODE_ROUNDED);
        return TextTree.newInstance(options).render(nodeStack.pop());
    }

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

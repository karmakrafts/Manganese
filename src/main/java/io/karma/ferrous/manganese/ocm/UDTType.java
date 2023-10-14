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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public enum UDTType {
    // @formatter:off
    STRUCT    (FerrousLexer.KW_STRUCT),
    CLASS     (FerrousLexer.KW_CLASS),
    ENUM_CLASS(FerrousLexer.KW_ENUM, FerrousLexer.KW_CLASS),
    ENUM      (FerrousLexer.KW_ENUM),
    TRAIT     (FerrousLexer.KW_TRAIT),
    ATTRIBUTE (FerrousLexer.KW_ATTRIB),
    INTERFACE (FerrousLexer.KW_INTERFACE);
    // @formatter:on

    private final int[] tokens;

    UDTType(final int... tokens) {
        this.tokens = tokens;
    }

    public int[] getTokens() {
        return tokens;
    }

    public String getText() {
        return Arrays.stream(tokens).mapToObj(TokenUtils::getLiteral).collect(Collectors.joining(" "));
    }
}

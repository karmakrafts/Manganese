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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public enum UDTKind {
    // @formatter:off
    STRUCT    (ScopeType.STRUCT,     FerrousLexer.KW_STRUCT),
    CLASS     (ScopeType.CLASS,      FerrousLexer.KW_CLASS),
    ENUM_CLASS(ScopeType.ENUM_CLASS, FerrousLexer.KW_ENUM, FerrousLexer.KW_CLASS),
    ENUM      (ScopeType.ENUM,       FerrousLexer.KW_ENUM),
    TRAIT     (ScopeType.TRAIT,      FerrousLexer.KW_TRAIT),
    ATTRIBUTE (ScopeType.ATTRIBUTE,  FerrousLexer.KW_ATTRIB),
    INTERFACE (ScopeType.INTERFACE,  FerrousLexer.KW_INTERFACE);
    // @formatter:on

    private final ScopeType scopeType;
    private final int[] tokens;

    UDTKind(final ScopeType scopeType, final int... tokens) {
        this.scopeType = scopeType;
        this.tokens = tokens;
    }

    public ScopeType getScopeType() {
        return scopeType;
    }

    public int[] getTokens() {
        return tokens;
    }

    public String getText() {
        return Arrays.stream(tokens).mapToObj(TokenUtils::getLiteral).collect(Collectors.joining(" "));
    }
}

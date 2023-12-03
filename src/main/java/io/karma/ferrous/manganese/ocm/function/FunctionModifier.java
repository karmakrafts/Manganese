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

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.FunctionModContext;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 02/12/2023
 */
@API(status = API.Status.INTERNAL)
public enum FunctionModifier {
    // @formatter:off
    CONST    (FerrousLexer.KW_CONST),
    INL      (FerrousLexer.KW_INL),
    VIRTUAL  (FerrousLexer.KW_VIRTUAL),
    OVERRIDE (FerrousLexer.KW_OVERRIDE),
    OP       (FerrousLexer.KW_OP),
    STATIC   (FerrousLexer.KW_STATIC),
    EXTERN   (FerrousLexer.KW_EXTERN),
    UNSAFE   (FerrousLexer.KW_UNSAFE);
    // @formatter:on

    private final int token;
    private final String text;

    FunctionModifier(final int token) {
        this.token = token;
        text = TokenUtils.getLiteral(token);
    }

    public static @Nullable FunctionModifier parse(final CompileContext compileContext,
                                                   final FunctionModContext context) {
        final var tokenType = context.start.getType();
        for (final var mod : values()) {
            if (mod.token != tokenType) {
                continue;
            }
            return mod;
        }
        return null;
    }

    public static EnumSet<FunctionModifier> parse(final CompileContext compileContext,
                                                  final Collection<FunctionModContext> contexts) {
        final var modifiers = EnumSet.noneOf(FunctionModifier.class);
        for (final var context : contexts) {
            final var mod = parse(compileContext, context);
            if (mod == null) {
                continue;
            }
            modifiers.add(mod);
        }
        return modifiers;
    }

    public String getText() {
        return text;
    }
}

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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import org.apiguardian.api.API;

import java.util.*;

/**
 * @author Alexander Hinze
 * @since 15/11/2023
 */
@API(status = API.Status.INTERNAL)
public enum TypeModifier {
    // @formatter:off
    ATOMIC  ('$', FerrousLexer.KW_ATOMIC),
    TLS     ('^', FerrousLexer.KW_TLS),
    MUT     ('=', FerrousLexer.KW_MUT),
    VOLATILE('!', FerrousLexer.KW_VOLATILE);
    // @formatter:on

    private final char mangledSymbol;
    private final String text;

    TypeModifier(final char mangledSymbol, final int token) {
        this.mangledSymbol = mangledSymbol;
        text = TokenUtils.getLiteral(token);
    }

    public static Optional<TypeModifier> parse(final FerrousParser.TypeModContext context) {
        return Arrays.stream(values()).filter(mod -> mod.text.equals(context.getText())).findFirst();
    }

    public static List<TypeModifier> parse(final CompileContext compileContext,
                                           final List<FerrousParser.TypeModContext> contexts) {
        if (contexts.isEmpty()) {
            return Collections.emptyList();
        }
        final var mods = new ArrayList<TypeModifier>();
        for (final var context : contexts) {
            final var mod = parse(context);
            if (mod.isEmpty()) {
                compileContext.reportError(context.start, CompileErrorCode.E3008);
                continue; // Report error but keep parsing
            }
            mods.add(mod.get());
        }
        return mods;
    }

    public char getMangledSymbol() {
        return mangledSymbol;
    }

    public String getText() {
        return text;
    }
}

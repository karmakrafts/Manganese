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

import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public record Identifier(String... components) {
    public static final Identifier EMPTY = new Identifier("");
    public static final String DELIMITER = TokenUtils.getLiteral(FerrousLexer.DOUBLE_COLON);

    public static Identifier format(final String fmt, final Object... params) {
        return new Identifier(String.format(fmt, params));
    }

    public static Identifier generate(final String prefix, final Identifier scopeName, boolean inheritScope,
                                      final Object... hashArgs) {
        var result = new Identifier(String.format("%s%d", prefix, Objects.hash(Objects.hash(hashArgs), scopeName)));
        if (inheritScope) {
            return scopeName.join(result);
        }
        return result;
    }

    public static Identifier generate(final String prefix, final ScopeStack scopeStack, boolean inheritScope,
                                      final Object... hashArgs) {
        return generate(prefix, scopeStack.getScopeName(), inheritScope, hashArgs);
    }

    public static Identifier parse(final String value) {
        if (!value.contains(DELIMITER)) {
            return new Identifier(value);
        }
        return new Identifier(value.split(DELIMITER));
    }

    public static Identifier parse(final ParserRuleContext context) {
        final var children = context.children;
        final var buffer = new StringBuilder();
        for (final var child : children) {
            if (child instanceof FerrousParser.LerpIdentContext lerpContext) {
                // TODO: handle interpolated identifiers
                Logger.INSTANCE.warn("Identifier interpolation is not implemented right now");
                continue;
            }
            buffer.append(child.getText());
        }
        return parse(buffer.toString());
    }

    public Identifier[] split(final String delimiter) {
        final var result = new ArrayList<Identifier>();
        for (final var comp : components) {
            result.addAll(Arrays.stream(comp.split(delimiter)).map(Identifier::parse).toList());
        }
        return result.toArray(Identifier[]::new);
    }

    public Identifier copy() {
        final var numElements = components.length;
        final var copied = new String[numElements];
        System.arraycopy(components, 0, copied, 0, numElements);
        return new Identifier(copied);
    }

    public Identifier join(final Identifier other) {
        if (isBlank()) {
            return other;
        }
        return parse(String.format("%s%s%s", this, DELIMITER, other));
    }

    public boolean isBlank() {
        return toString().isBlank();
    }

    public boolean isQualified() {
        return components.length > 1;
    }

    public String toInternalName() {
        return String.join(".", components);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Identifier ident) {
            return Arrays.equals(components, ident.components);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.join(DELIMITER, components);
    }
}

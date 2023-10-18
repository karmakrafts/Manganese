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
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public record Identifier(String... components) {
    public static final Identifier EMPTY = new Identifier("");
    public static final String DELIMITER = TokenUtils.getLiteral(FerrousLexer.DOUBLE_COLON);

    public static Identifier parse(final String value) {
        if (!value.contains(DELIMITER)) {
            return new Identifier(value);
        }
        return new Identifier(value.split(DELIMITER));
    }

    public Identifier[] split(final String delimiter) {
        final var result = new ArrayList<Identifier>();
        for (final var comp : components) {
            result.addAll(Arrays.stream(comp.split(delimiter)).map(Identifier::parse).toList());
        }
        return result.toArray(Identifier[]::new);
    }

    public Identifier join(final Identifier other, final char delimiter) {
        if (isBlank()) {
            return other;
        }
        return parse(String.format("%s%c%s", this, delimiter, other));
    }

    public Identifier join(final Identifier other, final String delimiter) {
        if (isBlank()) {
            return other;
        }
        return parse(String.format("%s%s%s", this, delimiter, other));
    }

    public boolean isBlank() {
        return toString().isBlank();
    }

    public boolean isQualified() {
        return components.length > 1;
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
        if (!isQualified()) {
            return components[0];
        }
        return String.join(DELIMITER, components);
    }
}

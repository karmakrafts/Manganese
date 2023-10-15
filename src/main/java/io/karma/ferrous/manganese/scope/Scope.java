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

package io.karma.ferrous.manganese.scope;

import io.karma.ferrous.manganese.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class Scope {
    public static final Scope GLOBAL = new Scope(ScopeType.GLOBAL, null);

    private final ScopeType type;
    private final Identifier name;

    public Scope(final ScopeType type, final @Nullable Identifier name) {
        this.type = type;
        this.name = name;
    }

    public Scope(final ScopeType type) {
        this(type, null);
    }

    public ScopeType getType() {
        return type;
    }

    public @Nullable Identifier getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Scope scope) {
            return type == scope.type && (name == null || name.equals(scope.name));
        }
        return false;
    }

    @Override
    public String toString() {
        if (name != null) {
            return String.format("%s [%s]", type, name);
        }
        return type.name();
    }
}

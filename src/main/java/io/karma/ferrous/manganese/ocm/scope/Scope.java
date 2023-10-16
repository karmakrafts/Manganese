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

package io.karma.ferrous.manganese.ocm.scope;

import io.karma.ferrous.manganese.util.Identifier;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class Scope implements EnclosingScopeProvider {
    public static final Scope GLOBAL = new Scope(ScopeType.GLOBAL);

    private final ScopeType type;
    private final Identifier name;
    private EnclosingScopeProvider enclosingScope = GLOBAL;

    public Scope(final ScopeType type, final Identifier name) {
        this.type = type;
        this.name = name;
    }

    public Scope(final ScopeType type) {
        this(type, Identifier.EMPTY);
    }

    @Override
    public Identifier getScopeName() {
        return name;
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final EnclosingScopeProvider scope) {
        this.enclosingScope = scope;
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

    public ScopeType getType() {
        return type;
    }

    @Override
    public Identifier getName() {
        return name;
    }
}

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
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class DefaultScope implements Scope {
    public static final DefaultScope GLOBAL = new DefaultScope(ScopeType.GLOBAL, Identifier.EMPTY);

    private final ScopeType type;
    private final Identifier name;
    private Scope enclosingScope;

    public DefaultScope(final ScopeType type, final Identifier name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public ScopeType getScopeType() {
        return type;
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public boolean equals(Object obj) {
        return switch(obj) { // @formatter:off
            case DefaultScope defScope  -> type == defScope.type && name.equals(defScope.name);
            case Scope scope            -> type == scope.getScopeType() && name.equals(scope.getName());
            default                     -> false;
        }; // @formatter:on
    }

    @Override
    public String toString() {
        if (name != null) {
            return String.format("%s [%s]", type, name);
        }
        return type.name();
    }
}

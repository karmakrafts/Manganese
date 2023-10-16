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

import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class ScopeStack extends Stack<ScopeProvider> {
    public static final ScopeStack EMPTY = new ScopeStack();

    public ScopeStack() {
    }

    public ScopeStack(final ScopeStack other) {
        addAll(other);
    }

    public <S extends ScopeProvider> S applyEnclosingScopes(final S provider) {
        ScopeProvider currentScope = provider;
        for (final var scope : reversed()) {
            currentScope.setEnclosingScope(scope);
            currentScope = scope;
        }
        return provider;
    }

    public Identifier getInternalName(final Identifier name) {
        return name;
    }
}

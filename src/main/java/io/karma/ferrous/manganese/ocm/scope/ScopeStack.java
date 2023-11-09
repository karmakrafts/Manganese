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

import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class ScopeStack extends Stack<Scope> {
    public ScopeStack() {
    }

    public ScopeStack(final ScopeStack other) {
        addAll(other);
    }

    public <S extends Scoped> S applyEnclosingScopes(final S provider) {
        Scoped currentScope = provider;
        for (final var scope : reversed()) {
            currentScope.setEnclosingScope(scope);
            currentScope = scope;
        }
        return provider;
    }

    public Identifier getScopeName() {
        var result = Identifier.EMPTY.copy();
        for (final var scope : this) {
            final var type = scope.getType();
            if (type == ScopeType.GLOBAL || type == ScopeType.FILE || type == ScopeType.MODULE_FILE) {
                continue;
            }
            result = result.join(scope.getName());
        }
        return result;
    }
}

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

import io.karma.ferrous.manganese.CompileError;
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.manganese.util.Identifier;

import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class ScopeStack extends Stack<Scope> {
    public Identifier getNestedName() {
        final var numScopes = size();
        final var buffer = new StringBuilder();
        for (var i = 0; i < numScopes; i++) {
            final var name = get(i).getName();
            if (name == null) {
                continue; // TODO: fix this to allow anon scopes
            }
            if (name.isQualified()) {
                throw new TranslationException(new CompileError("Qualified scope names are not supported right now"));
            }
            buffer.append(name);
            if (i < numScopes - 1) {
                buffer.append('.');
            }
        }
        return new Identifier(buffer.toString());
    }
}

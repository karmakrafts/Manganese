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

import io.karma.kommons.util.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
public final class ScopeUtils {
    // @formatter:off
    private ScopeUtils() {}
    // @formatter:on

    public static <T> @Nullable T findInScope(final Map<Identifier, T> map, final Identifier name,
                                              final Identifier scopeName) {
        var element = map.get(name);
        if (element == null) {
            // Attempt to resolve field types from the inside scope outwards
            final var partialScopeNames = scopeName.split("\\.");
            final var numPartialScopeNames = partialScopeNames.length;
            for (var j = numPartialScopeNames; j > 0; j--) {
                final var slicedScopeNames = ArrayUtils.slice(partialScopeNames, 0, j, Identifier[]::new);
                var currentScopeName = new Identifier("");
                for (final var partialScopeName : slicedScopeNames) {
                    currentScopeName = currentScopeName.join(partialScopeName, '.');
                }
                Logger.INSTANCE.debugln("Attempting to find '%s' in '%s'", name, currentScopeName);
                element = map.get(currentScopeName.join(name, '.'));
                if (element != null) {
                    break; // Stop if we found it
                }
            }
        }
        return element;
    }
}

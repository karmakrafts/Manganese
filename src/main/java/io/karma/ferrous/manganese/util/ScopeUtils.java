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
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public final class ScopeUtils {
    // @formatter:off
    private ScopeUtils() {}
    // @formatter:on

    public static <T> @Nullable T findInScope(final Map<Identifier, T> map, Identifier name,
                                              final Identifier scopeName) {
        T element = map.get(name);
        if (element == null) {
            final var components = scopeName.components();
            final var numComponents = components.length;
            for (var i = numComponents; i > 0; i--) {
                final var subComponents = ArrayUtils.slice(components, 0, i, String[]::new);
                element = map.get(new Identifier(subComponents).join(name));
                if (element != null) {
                    break; // Stop if we have found it
                }
            }
        }
        return element;
    }
}

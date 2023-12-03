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

import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Alexander Hinze
 * @since 27/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class Mangler {
    // @formatter:off
    private Mangler() {}
    // @formatter:on

    public static String mangleSequence(final Collection<Type> types) {
        final var builder = new StringBuilder();
        for (final var type : types) {
            builder.append(type.getMangledSequencePrefix()).append(type.getMangledName());
        }
        return builder.toString();
    }

    public static String mangleSequence(final Type... types) {
        return mangleSequence(Arrays.asList(types));
    }
}

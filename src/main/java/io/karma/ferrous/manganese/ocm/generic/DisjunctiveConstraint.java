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

package io.karma.ferrous.manganese.ocm.generic;

import io.karma.ferrous.manganese.ocm.type.Type;

/**
 * @author Alexander Hinze
 * @since 29/10/2023
 */
public record DisjunctiveConstraint(GenericConstraint... constraints) implements GenericConstraint {
    @Override
    public boolean test(final Type type) {
        for (final var constraint : constraints) {
            if (!constraint.test(type)) {
                continue;
            }
            return true;
        }
        return false;
    }
}

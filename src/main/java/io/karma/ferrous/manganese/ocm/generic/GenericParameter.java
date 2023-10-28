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

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
public final class GenericParameter {
    private final Identifier name;
    private final GenericConstraint constraints;
    private Expression value;

    public GenericParameter(final Identifier name, final GenericConstraint constraints,
                            final @Nullable Expression defaultValue) {
        this.name = name;
        this.constraints = constraints;
        value = defaultValue;
    }

    public GenericParameter(final Identifier name) {
        this(name, GenericConstraint.TRUE, null);
    }

    public Identifier getName() {
        return name;
    }

    public GenericConstraint getConstraints() {
        return constraints;
    }

    public @Nullable Expression getValue() {
        return value;
    }

    public void setValue(final @Nullable Expression value) {
        this.value = value;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(name, constraints, value);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GenericParameter param) { // @formatter:off
            return name.equals(param.name)
                && constraints.equals(param.constraints)
                && Objects.equals(value, param.value);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        if (value != null) {
            return String.format("%s:%s=%s", name, constraints, value);
        }
        return String.format("%s:%s", name, constraints);
    }
}

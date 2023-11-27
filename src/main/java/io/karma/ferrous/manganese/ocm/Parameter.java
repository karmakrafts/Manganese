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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public final class Parameter implements Named {
    private final Identifier name;
    private final Expression defaultValue;
    private Type type;

    public Parameter(final Identifier name, final Type type, final @Nullable Expression defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public @Nullable Expression getDefaultValue() {
        return defaultValue;
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Object

    @Override
    public String toString() {
        if (defaultValue == null) {
            return String.format("%s:%s", name, type);
        }
        return String.format("%s:%s=%s", name, type, defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, defaultValue);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Parameter param) { // @formatter:off
            return name.equals(param.name)
                && type.equals(param.type)
                && Objects.equals(defaultValue, param.defaultValue);
        } // @formatter:on
        return false;
    }
}

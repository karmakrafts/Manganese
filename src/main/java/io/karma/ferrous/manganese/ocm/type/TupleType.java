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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;

import java.util.Arrays;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
public final class TupleType implements Type {
    private final Type[] types;
    private Scope enclosingScope;

    public TupleType(final Type... types) {
        this.types = types;
    }

    public Type[] getTypes() {
        return types;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingScope = scope;
    }

    // Type

    @Override
    public GenericParameter[] getGenericParams() {
        return new GenericParameter[0];
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return NULL;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(types), enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof TupleType type) { // @formatter:off
            return Arrays.equals(types, type.types)
                && Objects.equals(enclosingScope, type.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append('(');
        final var numTypes = types.length;
        for (var i = 0; i < numTypes; i++) {
            buffer.append(types[i]);
            if (i < numTypes - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(')');
        return buffer.toString();
    }
}

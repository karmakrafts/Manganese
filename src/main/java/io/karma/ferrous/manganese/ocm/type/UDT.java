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

import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;

import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
public record UDT(UDTKind kind, StructureType type, List<Field> fields, TokenSlice tokenSlice) implements NamedType {
    // NameProvider

    @Override
    public Identifier getName() {
        return type.getName();
    }

    // Type

    @Override
    public GenericParameter[] getGenericParams() {
        return type.getGenericParams();
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return type.materialize(machine);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return type.getBaseType();
    }

    @Override
    public Scope getEnclosingScope() {
        return type.getEnclosingScope();
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        type.setEnclosingScope(scope);
    }

    // Object

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UDT udt) {
            return kind == udt.kind && Objects.equals(type, udt.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return type == null ? "null" : type.toString();
    }
}

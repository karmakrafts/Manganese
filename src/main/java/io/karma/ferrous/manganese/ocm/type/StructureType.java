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

import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class StructureType implements Type {
    private final Identifier name;
    private final boolean isPacked;
    private final Type[] fieldTypes;
    private long materializedType = MemoryUtil.NULL;
    private Scope enclosingScope;

    StructureType(final Identifier name, final boolean isPacked, final Type... fieldTypes) {
        this.name = name;
        this.isPacked = isPacked;
        this.fieldTypes = fieldTypes;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public void setFieldType(final int index, final Type type) {
        fieldTypes[index] = type;
    }

    public Type getFieldType(final int index) {
        return fieldTypes[index];
    }

    public List<Type> getFieldTypes() {
        return Arrays.asList(fieldTypes);
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingScope = scope;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public boolean isComplete() {
        for (final var fieldType : fieldTypes) {
            if (fieldType.isComplete()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        try (final var stack = MemoryStack.stackPush()) {
            final var numFields = fieldTypes.length;
            final var fields = stack.callocPointer(numFields);
            for (var i = 0; i < numFields; i++) {
                fields.put(i, fieldTypes[i].materialize(machine));
            }
            materializedType = LLVMCore.LLVMStructCreateNamed(LLVMGetGlobalContext(), getInternalName().toString());
            LLVMCore.LLVMStructSetBody(materializedType, fields, isPacked);
            return materializedType;
        }
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInternalName(), isPacked, Arrays.hashCode(fieldTypes));
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StructureType type) { // @formatter:off
            return getInternalName().equals(type.getInternalName())
                && isPacked == type.isPacked
                && Arrays.equals(fieldTypes, type.fieldTypes);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s:%b:%s", getInternalName(), isPacked, Arrays.toString(fieldTypes));
    }
}

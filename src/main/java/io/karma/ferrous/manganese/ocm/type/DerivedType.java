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

import io.karma.ferrous.manganese.ocm.scope.EnclosingScopeProvider;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class DerivedType implements Type {
    private final Type baseType;
    private final TypeAttribute[] attributes;
    private long materializedType = MemoryUtil.NULL;

    DerivedType(Type baseType, TypeAttribute... attributes) {
        this.baseType = baseType;
        this.attributes = attributes;
    }

    @Override
    public Identifier getName() {
        return baseType.getName();
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return baseType.getEnclosingScope();
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public long materialize(final Target target) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        materializedType = baseType.materialize(target);
        for (var i = 0; i < attributes.length; i++) {
            materializedType = LLVMCore.LLVMPointerType(materializedType, 0);
        }
        return materializedType;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return attributes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseType, Arrays.hashCode(attributes));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DerivedType type) {
            return baseType.equals(type.baseType) && Arrays.equals(attributes, type.attributes);
        }
        return false;
    }

    @Override
    public String toString() {
        var result = baseType.toString();
        for (final var attrib : attributes) {
            result = attrib.format(result);
        }
        return result;
    }
}

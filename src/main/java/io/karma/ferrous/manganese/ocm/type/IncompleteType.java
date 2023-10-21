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
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class IncompleteType implements Type {
    private final Identifier name;
    private long materializedType = MemoryUtil.NULL;
    private Scope enclosingType;

    IncompleteType(final Identifier name) {
        this.name = name;
    }

    @Override
    public ScopeType getScopeType() {
        return enclosingType.getScopeType();
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingType;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingType = scope;
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
        return false;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        return materializedType = LLVMCore.LLVMStructCreateNamed(LLVMGetGlobalContext(), getInternalName().toString());
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public int hashCode() {
        return getInternalName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IncompleteType type) {
            return getInternalName().equals(type.getInternalName());
        }
        return false;
    }

    @Override
    public String toString() {
        return getInternalName().toString();
    }
}

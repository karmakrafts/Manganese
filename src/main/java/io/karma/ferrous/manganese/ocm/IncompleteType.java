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

import io.karma.ferrous.manganese.scope.ScopeProvider;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class IncompleteType implements Type {
    private final Identifier identifier;
    private long materializedType = MemoryUtil.NULL;
    private ScopeProvider enclosingType;

    IncompleteType(final Identifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public ScopeProvider getEnclosingScope() {
        return enclosingType;
    }

    @Override
    public void setEnclosingScope(final ScopeProvider scope) {
        enclosingType = scope;
    }

    @Override
    public Identifier getName() {
        return identifier;
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
    public long materialize(final Target target) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        return materializedType = LLVMCore.LLVMStructCreateNamed(LLVMGetGlobalContext(), identifier.toString());
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
        return identifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IncompleteType type) {
            return identifier.equals(type.identifier);
        }
        return false;
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}

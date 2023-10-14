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

package io.karma.ferrous.manganese.type;

import io.karma.ferrous.manganese.target.Target;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;

import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class FunctionType {
    private final Type returnType;
    private final List<Type> paramTypes;
    private final boolean isVarArg;

    public FunctionType(final Type returnType, final List<Type> paramTypes, final boolean isVarArg) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
        this.isVarArg = isVarArg;
    }

    public FunctionType(final Type returnType, final List<Type> paramTypes) {
        this(returnType, paramTypes, false);
    }

    public long materialize(final Target target) {
        try (final var stack = MemoryStack.stackPush()) {
            final var returnType = this.returnType.materialize(target);
            final var paramTypes = this.paramTypes.stream().mapToLong(type -> type.materialize(target)).toArray();
            return LLVMCore.LLVMFunctionType(returnType, stack.pointers(paramTypes), isVarArg);
        }
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public String toString(final @Nullable String name) {
        final var builder = new StringBuilder();
        builder.append(returnType);

        if (name != null) {
            builder.append(' ');
            builder.append(name);
        }

        builder.append('(');
        final var numParams = paramTypes.size();
        for (var i = 0; i < numParams; i++) {
            builder.append(paramTypes.get(i));
            if (i < numParams - 1) {
                builder.append(", ");
            }
        }

        if (isVarArg) {
            builder.append(", ...");
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, paramTypes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FunctionType type) {
            return returnType.equals(type.returnType) && paramTypes.equals(type.paramTypes);
        }
        return false;
    }

    @Override
    public String toString() {
        return toString(null);
    }
}

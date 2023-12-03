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

package io.karma.ferrous.manganese.ee;

import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeKind;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;

import static org.lwjgl.llvm.LLVMExecutionEngine.LLVMCreateGenericValueOfInt;
import static org.lwjgl.llvm.LLVMExecutionEngine.LLVMDisposeGenericValue;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class IntValue implements GenericValue {
    private final Type type;
    private final long address;
    private final boolean isAllocated;
    private boolean isDisposed;

    IntValue(final Type type, final long address) {
        this.type = type;
        this.address = address;
        isAllocated = false;
    }

    IntValue(final TargetMachine targetMachine, final Type type, final long value) {
        this.type = type;
        final var typeAddress = type.materialize(targetMachine);
        address = LLVMCreateGenericValueOfInt(typeAddress, value, type.getKind() == TypeKind.INT);
        if (address == NULL) {
            throw new IllegalStateException("Could not allocate generic int value");
        }
        isAllocated = true;
    }

    public boolean isSigned() {
        return type.getKind() == TypeKind.INT;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public void dispose() {
        if (!isAllocated || isDisposed) {
            return;
        }
        LLVMDisposeGenericValue(address);
        isDisposed = true;
    }
}

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

import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;

import static org.lwjgl.llvm.LLVMExecutionEngine.LLVMCreateGenericValueOfPointer;
import static org.lwjgl.llvm.LLVMExecutionEngine.LLVMDisposeGenericValue;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class PointerValue implements GenericValue {
    private final Type type;
    private final long address;
    private final boolean isAllocated;
    private boolean isDisposed;

    PointerValue(long address) {
        type = BuiltinType.VOID;
        this.address = address;
        isAllocated = false;
    }

    PointerValue(final Type type, final long value) {
        this.type = type;
        address = LLVMCreateGenericValueOfPointer(value);
        if (address == NULL) {
            throw new IllegalStateException("Could not allocate generic pointer value");
        }
        isAllocated = true;
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

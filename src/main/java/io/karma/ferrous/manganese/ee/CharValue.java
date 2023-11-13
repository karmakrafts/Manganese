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
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMExecutionEngine;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class CharValue implements GenericValue {
    private final long address;
    private final boolean isAllocated;
    private boolean isDisposed;

    CharValue(final long address) {
        this.address = address;
        isAllocated = false;
    }

    CharValue(final TargetMachine targetMachine, final char value) {
        final var typeAddress = BuiltinType.CHAR.materialize(targetMachine);
        address = LLVMExecutionEngine.LLVMCreateGenericValueOfInt(typeAddress, value, true);
        if (address == NULL) {
            throw new IllegalStateException("Could not allocate generic char value");
        }
        isAllocated = true;
    }

    @Override
    public Type getType() {
        return BuiltinType.CHAR;
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
        LLVMExecutionEngine.LLVMDisposeGenericValue(address);
        isDisposed = true;
    }
}

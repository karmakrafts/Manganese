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

package io.karma.ferrous.manganese.ocm.ir;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.LLVMAddIncoming;
import static org.lwjgl.llvm.LLVMCore.LLVMBuildPhi;

/**
 * @author Alexander Hinze
 * @since 15/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class PhiBuilder {
    private final long builderAddress;
    private final IRContext irContext;
    private final Long2LongLinkedOpenHashMap targets = new Long2LongLinkedOpenHashMap();
    private long type;

    PhiBuilder(final long builderAddress, final IRContext irContext) {
        this.builderAddress = builderAddress;
        this.irContext = irContext;
    }

    public PhiBuilder setType(final long type) {
        this.type = type;
        return this;
    }

    public PhiBuilder addIncoming(final long block, final long value) {
        if (targets.containsKey(block)) {
            return this;
        }
        targets.put(block, value);
        return this;
    }

    public PhiBuilder addIncoming(final String name, final long value) {
        final var blockAddress = irContext.get(name).getBlockAddress();
        if (targets.containsKey(blockAddress)) {
            return this;
        }
        targets.put(blockAddress, value);
        return this;
    }

    public long build() {
        final var address = LLVMBuildPhi(builderAddress, type, "");
        final var blocks = targets.keySet().toLongArray();
        final var values = targets.values().toLongArray();
        try (final var stack = MemoryStack.stackPush()) {
            LLVMAddIncoming(address, stack.pointers(values), stack.pointers(blocks));
        }
        return address;
    }
}

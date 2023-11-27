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

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;

/**
 * @author Alexander Hinze
 * @since 15/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class PhiBuilder {
    private final IRContext irContext;
    private final long address;
    private final Long2LongOpenHashMap targets = new Long2LongOpenHashMap();

    PhiBuilder(final IRContext irContext, final long address) {
        this.irContext = irContext;
        this.address = address;
    }

    public void addIncoming(final long block, final long value) {
        if (targets.containsKey(block)) {
            return;
        }
        targets.put(block, value);
    }

    public void addIncoming(final String name, final long value) {
        final var blockAddress = irContext.getOrCreate(name).getBlockAddress();
        if (targets.containsKey(blockAddress)) {
            return;
        }
        targets.put(blockAddress, value);
    }

    public void build() {
        final var blocks = targets.keySet().toLongArray();
        final var values = targets.values().toLongArray();
        try (final var stack = MemoryStack.stackPush()) {
            LLVMCore.LLVMAddIncoming(address, stack.pointers(values), stack.pointers(blocks));
        }
    }
}

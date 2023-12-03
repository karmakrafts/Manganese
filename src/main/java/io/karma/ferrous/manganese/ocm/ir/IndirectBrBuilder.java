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

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.apiguardian.api.API;

import static org.lwjgl.llvm.LLVMCore.LLVMAddDestination;
import static org.lwjgl.llvm.LLVMCore.LLVMBuildIndirectBr;

/**
 * @author Alexander Hinze
 * @since 30/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class IndirectBrBuilder {
    private final long builderAddress;
    private final LongArrayList destinations = new LongArrayList();
    private long address;

    IndirectBrBuilder(final long builderAddress) {
        this.builderAddress = builderAddress;
    }

    public IndirectBrBuilder setAddress(final long address) {
        this.address = address;
        return this;
    }

    public IndirectBrBuilder addDestination(final long destination) {
        if (destinations.contains(destination)) {
            return this;
        }
        destinations.add(destination);
        return this;
    }

    public long build() {
        final var address = LLVMBuildIndirectBr(builderAddress, this.address, destinations.size());
        for (final var destination : destinations) {
            LLVMAddDestination(address, destination);
        }
        return address;
    }
}

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

package io.karma.ferrous.manganese.target;

import io.karma.ferrous.manganese.util.LLVMUtils;
import io.karma.ferrous.manganese.util.Logger;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMTarget;
import org.lwjgl.llvm.LLVMTargetMachine;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public final class Target {
    private final Architecture architecture;
    private final Platform platform;
    private final ABI abi;
    private final long address;
    private final long dataAddress;
    private boolean isDisposed = false;

    public Target(final Architecture architecture, final Platform platform, final ABI abi) {
        this.architecture = architecture;
        this.platform = platform;
        this.abi = abi;

        try (final var stack = MemoryStack.stackPush()) {
            final var buffer = stack.callocPointer(1);
            final var messageBuffer = stack.callocPointer(1);
            if (!LLVMTargetMachine.LLVMGetTargetFromTriple(toString(), buffer, messageBuffer)) {
                LLVMUtils.checkStatus(messageBuffer);
            }
            address = buffer.get(0);
            if (address == MemoryUtil.NULL) {
                throw new RuntimeException("Could not retrieve target address");
            }
        }

        dataAddress = LLVMTarget.LLVMCreateTargetData(toString());
        if (dataAddress == MemoryUtil.NULL) {
            throw new RuntimeException("Could not retrieve target data address");
        }

        Logger.INSTANCE.debugln("Allocated target %s at 0x%08X [0x%08X]", toString(), address, dataAddress);
    }

    public static Target getHostTarget() {
        return new Target(Architecture.getHostArchitecture(), Platform.getHostPlatform(), ABI.getHostABI());
    }

    public TargetMachine createMachine(final String features, final OptimizationLevel level, final Relocation reloc,
                                       final CodeModel model) {
        return new TargetMachine(this, features, level, reloc, model);
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        LLVMTarget.LLVMDisposeTargetData(dataAddress);
        Logger.INSTANCE.debugln("Disposed target %s at 0x%08X [0x%08X]", toString(), address, dataAddress);
        isDisposed = true;
    }

    public int getPointerSize() {
        return LLVMTarget.LLVMPointerSize(dataAddress);
    }

    public long getDataAddress() {
        return dataAddress;
    }

    public long getAddress() {
        return address;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public Platform getPlatform() {
        return platform;
    }

    public ABI getABI() {
        return abi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(architecture, platform, abi);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Target target) { // @formatter:off
            return architecture == target.architecture
                && platform == target.platform
                && abi == target.abi;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s-%s-%s", architecture.getName(), platform.getName(), abi.getName());
    }
}

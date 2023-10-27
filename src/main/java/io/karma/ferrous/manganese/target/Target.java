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

import io.karma.ferrous.manganese.llvm.LLVMUtils;
import io.karma.ferrous.manganese.util.Logger;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.system.MemoryStack;

import java.util.Objects;
import java.util.Optional;

import static org.lwjgl.llvm.LLVMTargetMachine.LLVMGetTargetFromTriple;
import static org.lwjgl.llvm.LLVMTargetMachine.LLVMNormalizeTargetTriple;
import static org.lwjgl.system.MemoryUtil.NULL;

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

    @API(status = Status.INTERNAL)
    public Target(final Architecture architecture, final Platform platform, final ABI abi) {
        this.architecture = architecture;
        this.platform = platform;
        this.abi = abi;

        final var triple = getNormalizedTriple();
        try (final var stack = MemoryStack.stackPush()) {
            final var buffer = stack.callocPointer(1);
            final var messageBuffer = stack.callocPointer(1);
            if (LLVMGetTargetFromTriple(triple, buffer, messageBuffer)) {
                LLVMUtils.checkStatus(messageBuffer);
            }
            address = buffer.get(0);
            if (address == NULL) {
                throw new RuntimeException("Could not retrieve target address");
            }
        }
        Logger.INSTANCE.debugln("Allocated target %s at 0x%08X", triple, address);
    }

    public static Target getHostTarget() {
        return new Target(Architecture.getHostArchitecture(), Platform.getHostPlatform(), ABI.getHostABI());
    }

    public static String getHostTargetTriple() {
        return String.format("%s-%s-%s", Architecture.getHostArchitecture().getName(),
                Platform.getHostPlatform().getName(), ABI.getHostABI().getName());
    }

    public static Optional<Target> parse(final String value) {
        final var parts = value.split("-");
        if (parts.length == 0) {
            return Optional.empty();
        }
        final var arch = Architecture.byName(parts[0]);
        // @formatter:off
        final var platform = parts.length >= 2
            ? Platform.byName(parts[1])
            : Optional.of(Platform.getHostPlatform());
        final var abi = parts.length >= 3
            ? ABI.byName(parts[2])
            : Optional.of(ABI.getHostABI());
        // @formatter:on
        if (arch.isEmpty() || platform.isEmpty() || abi.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Target(arch.get(), platform.get(), abi.get()));
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

    public String getNormalizedTriple() {
        return Objects.requireNonNull(LLVMNormalizeTargetTriple(toString()));
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

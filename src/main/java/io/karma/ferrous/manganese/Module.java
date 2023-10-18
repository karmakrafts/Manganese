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

package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.llvm.LLVMAnalysis.LLVMReturnStatusAction;
import static org.lwjgl.llvm.LLVMAnalysis.LLVMVerifyModule;
import static org.lwjgl.llvm.LLVMBitReader.LLVMParseBitcodeInContext;
import static org.lwjgl.llvm.LLVMBitWriter.LLVMWriteBitcodeToMemoryBuffer;
import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.llvm.LLVMIRReader.LLVMParseIRInContext;
import static org.lwjgl.llvm.LLVMLinker.LLVMLinkModules2;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 17/10/2023
 */
@API(status = Status.INTERNAL)
public final class Module {
    private final long context;
    private final long address;
    private boolean isDisposed = false;

    public Module(final String name, final long context) {
        this.context = context;
        address = LLVMModuleCreateWithNameInContext(name, context);
        if (address == MemoryUtil.NULL) {
            throw new RuntimeException("Could not allocate module");
        }
    }

    public Module(final String name) {
        this(name, LLVMGetGlobalContext());
    }

    private Module(final long context, final long address) {
        this.context = context;
        this.address = address;
    }

    private static void checkStatus(final PointerBuffer buffer) throws RuntimeException {
        final var message = buffer.get(0);
        if (message != NULL) {
            final var result = buffer.getStringUTF8(0);
            nLLVMDisposeMessage(message);
            throw new RuntimeException(result);
        }
        throw new RuntimeException("Unknown error");
    }

    public static Module fromIR(final long context, final String name, final String source) throws RuntimeException {
        try (final var stack = MemoryStack.stackPush()) {
            final var buffer = stack.callocPointer(1);
            final var messageBuffer = stack.callocPointer(1);
            final var sourceBuffer = stack.UTF8(source, true);
            final var sourceAddr = MemoryUtil.memAddress(sourceBuffer);
            if (!LLVMParseIRInContext(context, sourceAddr, buffer, messageBuffer)) {
                checkStatus(messageBuffer);
            }
            final var bufferAddr = buffer.get(0);
            if (bufferAddr == NULL) {
                throw new RuntimeException("Could not retrieve bitcode address");
            }
            final var moduleBuffer = stack.callocPointer(1);
            if (!LLVMParseBitcodeInContext(context, bufferAddr, moduleBuffer, messageBuffer)) {
                checkStatus(messageBuffer);
            }
            final var moduleAddr = moduleBuffer.get(0);
            if (moduleAddr == NULL) {
                throw new RuntimeException("Could not retrieve module address");
            }
            final var module = new Module(context, moduleAddr);
            module.setName(name);
            return module;
        }
    }

    public static Module loadEmbedded(final long context, final String name) throws IOException {
        // @formatter:off
        try(final var stream = Module.class.getResourceAsStream(String.format("/%s.ll", name));
            final var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))) {
            // @formatter:on
            final var source = reader.lines().collect(Collectors.joining("\n"));
            return fromIR(context, name, source);
        }
    }

    public @Nullable String verify() {
        try (final var stack = MemoryStack.stackPush()) {
            final var messageBuffer = stack.callocPointer(1);
            if (LLVMVerifyModule(address, LLVMReturnStatusAction, messageBuffer)) {
                final var message = messageBuffer.get(0);
                if (message != NULL) {
                    final var result = messageBuffer.getStringUTF8(0);
                    nLLVMDisposeMessage(message);
                    return result;
                }
                return "Unknown error";
            }
            return null;
        }
    }

    public void linkInto(final Module module) {
        LLVMLinkModules2(module.address, address);
    }

    public Module linkWith(final Module module) {
        linkInto(this);
        return this;
    }

    public String disassemble() {
        return LLVMPrintModuleToString(address);
    }

    public String getName() {
        return LLVMGetModuleIdentifier(address);
    }

    public void setName(final String name) {
        LLVMSetModuleIdentifier(address, name);
    }

    public String getSourceFileName() {
        return LLVMGetSourceFileName(address);
    }

    public void setSourceFileName(final String fileName) {
        LLVMSetSourceFileName(address, fileName);
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        LLVMDisposeModule(address);
        isDisposed = true;
    }

    public long getContext() {
        return context;
    }

    public long getAddress() {
        return address;
    }

    public @Nullable ByteBuffer getBitcode() {
        final var buffer = LLVMWriteBitcodeToMemoryBuffer(address);
        Logger.INSTANCE.debugln("Wrote bitcode to memory at 0x%08X", buffer);
        if (buffer == NULL) {
            return null;
        }
        final var size = (int) LLVMGetBufferSize(buffer);
        final var address = nLLVMGetBufferStart(buffer); // LWJGL codegen is a pita
        final var result = MemoryUtil.memByteBuffer(address, size);
        LLVMDisposeMemoryBuffer(buffer);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, address, isDisposed);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Module module) {
            return context == module.context && address == module.address && isDisposed == module.isDisposed;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Module 0x%08X (in 0x%08X)", address, context);
    }
}

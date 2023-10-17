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
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMBitWriter;
import org.lwjgl.llvm.LLVMLinker;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.LLVMDisposeMemoryBuffer;
import static org.lwjgl.llvm.LLVMCore.LLVMDisposeModule;
import static org.lwjgl.llvm.LLVMCore.LLVMGetBufferSize;
import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;
import static org.lwjgl.llvm.LLVMCore.LLVMGetModuleIdentifier;
import static org.lwjgl.llvm.LLVMCore.LLVMGetSourceFileName;
import static org.lwjgl.llvm.LLVMCore.LLVMModuleCreateWithNameInContext;
import static org.lwjgl.llvm.LLVMCore.LLVMPrintModuleToString;
import static org.lwjgl.llvm.LLVMCore.LLVMSetSourceFileName;
import static org.lwjgl.llvm.LLVMCore.nLLVMGetBufferStart;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 17/10/2023
 */
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

    public String disassemble() {
        return LLVMPrintModuleToString(address);
    }

    public String getName() {
        return LLVMGetModuleIdentifier(address);
    }

    public String getSourceFileName() {
        return LLVMGetSourceFileName(address);
    }

    public void setSourceFileName(final String fileName) {
        LLVMSetSourceFileName(address, fileName);
    }

    public void linkInto(final Module module) {
        LLVMLinker.LLVMLinkModules2(module.address, address);
    }

    public Module linkWith(final Module module) {
        linkInto(this);
        return this;
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
        final var buffer = LLVMBitWriter.LLVMWriteBitcodeToMemoryBuffer(address);
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

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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class BlockBuilder {
    private final BlockContext blockContext;
    private final Module module;
    private final TargetMachine targetMachine;
    private final long blockAddress;
    private final long address;
    private boolean isDisposed = false;

    public BlockBuilder(final BlockContext blockContext, final Module module, final TargetMachine targetMachine,
                        final long blockAddress, final long context) {
        this.blockContext = blockContext;
        this.module = module;
        this.targetMachine = targetMachine;
        if (blockAddress == NULL || context == NULL) {
            throw new IllegalArgumentException("Block address and/or context cannot be null");
        }
        this.blockAddress = blockAddress;
        address = LLVMCreateBuilderInContext(context);
        if (address == NULL) {
            throw new IllegalStateException("Could not allocate builder instance");
        }
        LLVMPositionBuilderAtEnd(address, blockAddress);
    }

    // Integer arithmetics

    public long add(final long lhs, final long rhs) {
        return LLVMBuildAdd(address, lhs, rhs, "");
    }

    public long sub(final long lhs, final long rhs) {
        return LLVMBuildSub(address, lhs, rhs, "");
    }

    public long mul(final long lhs, final long rhs) {
        return LLVMBuildMul(address, lhs, rhs, "");
    }

    public long sdiv(final long lhs, final long rhs) {
        return LLVMBuildSDiv(address, lhs, rhs, "");
    }

    public long srem(final long lhs, final long rhs) {
        return LLVMBuildSRem(address, lhs, rhs, "");
    }

    public long udiv(final long lhs, final long rhs) {
        return LLVMBuildUDiv(address, lhs, rhs, "");
    }

    public long urem(final long lhs, final long rhs) {
        return LLVMBuildURem(address, lhs, rhs, "");
    }

    public long neg(final long value) {
        return LLVMBuildNeg(address, value, "");
    }

    // Floating point arithmetics

    public long fadd(final long lhs, final long rhs) {
        return LLVMBuildFAdd(address, lhs, rhs, "");
    }

    public long fsub(final long lhs, final long rhs) {
        return LLVMBuildFSub(address, lhs, rhs, "");
    }

    public long fmul(final long lhs, final long rhs) {
        return LLVMBuildFMul(address, lhs, rhs, "");
    }

    public long fdiv(final long lhs, final long rhs) {
        return LLVMBuildFDiv(address, lhs, rhs, "");
    }

    public long frem(final long lhs, final long rhs) {
        return LLVMBuildFRem(address, lhs, rhs, "");
    }

    public long fneg(final long value) {
        return LLVMBuildFNeg(address, value, "");
    }

    // Control flow

    public long jump(final String name) {
        return LLVMBuildBr(address, blockContext.getOrCreate(name).blockAddress);
    }

    public long call(final Function function, final long... args) {
        final var fnAddress = function.materialize(blockContext.getCompileContext(), module, targetMachine);
        final var returnType = function.getType().getReturnType();
        try (final var stack = MemoryStack.stackPush()) {
            final var typeAddress = returnType.materialize(targetMachine);
            return LLVMBuildCall(address, fnAddress, stack.pointers(args), "");
        }
    }

    public long ret(final long value) {
        return LLVMBuildRet(address, value);
    }

    public long ret() {
        return LLVMBuildRetVoid(address);
    }

    public long getAddress() {
        return address;
    }

    public long getBlockAddress() {
        return blockAddress;
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        LLVMDisposeBuilder(address);
        isDisposed = true;
    }
}

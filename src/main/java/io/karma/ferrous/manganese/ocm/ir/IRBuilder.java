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

import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.function.Function;
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
public final class IRBuilder {
    private final IRContext blockContext;
    private final Module module;
    private final TargetMachine targetMachine;
    private final long blockAddress;
    private final long address;
    private boolean isDisposed = false;

    public IRBuilder(final IRContext blockContext, final Module module, final TargetMachine targetMachine,
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

    // Memory

    public Allocation alloca(final long type) {
        return new Allocation(AllocationKind.STACK, LLVMBuildAlloca(address, type, ""));
    }

    public Allocation arrayAlloca(final long type, final long value) {
        return new Allocation(AllocationKind.STACK_ARRAY, LLVMBuildArrayAlloca(address, type, value, ""));
    }

    public Allocation malloc(final long type) {
        return new Allocation(AllocationKind.HEAP, LLVMBuildMalloc(address, type, ""));
    }

    public Allocation arrayMalloc(final long type, final long value) {
        return new Allocation(AllocationKind.HEAP_ARRAY, LLVMBuildArrayMalloc(address, type, value, ""));
    }

    public long free(final long ptr) {
        return LLVMBuildFree(address, ptr);
    }

    public long memset(final long ptr, final long value, final long size, final int alignment) {
        return LLVMBuildMemSet(address, ptr, value, size, alignment);
    }

    public long memcpy(final long dst, final int dstAlignment, final long src, final int srcAlignment,
                       final long size) {
        return LLVMBuildMemCpy(address, dst, dstAlignment, src, srcAlignment, size);
    }

    public long memmove(final long dst, final int dstAlignment, final long src, final int srcAlignment,
                        final long size) {
        return LLVMBuildMemMove(address, dst, dstAlignment, src, srcAlignment, size);
    }

    // Load/store

    public long load(final long type, final long ptr) {
        return LLVMBuildLoad2(address, type, ptr, "");
    }

    public long store(final long value, final long ptr) {
        return LLVMBuildStore(address, value, ptr);
    }

    // Conversions

    public long intToPtr(final long type, final long value) {
        return LLVMBuildIntToPtr(address, value, type, "");
    }

    public long ptrToInt(final long type, final long value) {
        return LLVMBuildPtrToInt(address, value, type, "");
    }

    // Strings

    public long str(final String value) {
        return LLVMBuildGlobalString(address, value, "");
    }

    public long strPtr(final String value) {
        return LLVMBuildGlobalStringPtr(address, value, "");
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

    // Bitwise operations

    public long and(final long lhs, final long rhs) {
        return LLVMBuildAnd(address, lhs, rhs, "");
    }

    public long or(final long lhs, final long rhs) {
        return LLVMBuildOr(address, lhs, rhs, "");
    }

    public long xor(final long lhs, final long rhs) {
        return LLVMBuildXor(address, lhs, rhs, "");
    }

    public long shl(final long value, final long count) {
        return LLVMBuildShl(address, value, count, "");
    }

    public long ashr(final long value, final long count) {
        return LLVMBuildAShr(address, value, count, "");
    }

    public long lshr(final long value, final long count) {
        return LLVMBuildLShr(address, value, count, "");
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
        try (final var stack = MemoryStack.stackPush()) {
            final var fnAddress = function.materialize(blockContext.getCompileContext(), module, targetMachine);
            final var typeAddress = function.getType().materialize(targetMachine);
            return LLVMBuildCall2(address, typeAddress, fnAddress, stack.pointers(args), "");
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

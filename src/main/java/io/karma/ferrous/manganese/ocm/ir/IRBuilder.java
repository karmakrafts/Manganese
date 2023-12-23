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

import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.IntrinsicFunction;
import io.karma.ferrous.manganese.ocm.type.BoolType;
import io.karma.ferrous.manganese.ocm.type.TypeModifier;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.EnumCompareOp;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class IRBuilder implements AutoCloseable {
    private final IRContext irContext;
    private final TargetMachine targetMachine;
    private final long blockAddress;
    private final long address;
    private boolean isDisposed = false;

    public IRBuilder(final IRContext irContext, final TargetMachine targetMachine, final long blockAddress,
                     final long context) {
        this.irContext = irContext;
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

    public long alloca(final long type) {
        return LLVMBuildAlloca(address, type, "");
    }

    public long arrayAlloca(final long type, final long value) {
        return LLVMBuildArrayAlloca(address, type, value, "");
    }

    public long malloc(final long type) {
        return LLVMBuildMalloc(address, type, "");
    }

    public long arrayMalloc(final long type, final long value) {
        return LLVMBuildArrayMalloc(address, type, value, "");
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

    public long extract(final long value, final int index) {
        return LLVMBuildExtractValue(address, value, index, "");
    }

    public long insert(final long value, final long fieldValue, final int index) {
        return LLVMBuildInsertValue(address, value, fieldValue, index, "");
    }

    public long gep(final long type, final long ptr, final int index) {
        return LLVMBuildStructGEP2(this.address, type, ptr, index, "");
    }

    // Load/store

    public long load(final long type, final long ptr, final TypeModifier... modifiers) {
        return LLVMBuildLoad2(address, type, ptr, "");
    }

    public long loadAtomic(final long value, final long ptr, final int ordering) {
        return LLVMBuildAtomicRMW(address, LLVMLoad, ptr, NULL, ordering, false);
    }

    public long store(final long value, final long ptr) {
        return LLVMBuildStore(address, value, ptr);
    }

    public long storeAtomic(final long value, final long ptr, final int ordering) {
        return LLVMBuildAtomicRMW(address, LLVMStore, ptr, value, ordering, false);
    }

    // Conversions

    public long sintToFloat(final long type, final long value) {
        return LLVMBuildSIToFP(address, value, type, "");
    }

    public long uintToFloat(final long type, final long value) {
        return LLVMBuildUIToFP(address, value, type, "");
    }

    public long floatToSint(final long type, final long value) {
        return LLVMBuildFPToSI(address, value, type, "");
    }

    public long floatToUint(final long type, final long value) {
        return LLVMBuildFPToUI(address, value, type, "");
    }

    public long sintCast(final long type, final long value) {
        return LLVMBuildIntCast2(address, value, type, true, "");
    }

    public long uintCast(final long type, final long value) {
        return LLVMBuildIntCast2(address, value, type, false, "");
    }

    public long intToPtr(final long type, final long value) {
        return LLVMBuildIntToPtr(address, value, type, "");
    }

    public long ptrToInt(final long type, final long value) {
        return LLVMBuildPtrToInt(address, value, type, "");
    }

    public long floatTrunc(final long type, final long value) {
        return LLVMBuildFPTrunc(address, value, type, "");
    }

    public long floatExt(final long type, final long value) {
        return LLVMBuildFPExt(address, value, type, "");
    }

    public long trunc(final long type, final long value) {
        return LLVMBuildTrunc(address, value, type, "");
    }

    public long sext(final long type, final long value) {
        return LLVMBuildSExt(address, value, type, "");
    }

    public long zext(final long type, final long value) {
        return LLVMBuildZExt(address, value, type, "");
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

    // Control flow

    public long condBr(final long condition, final String trueLabel, final String falseLabel) {
        final var trueAddress = irContext.get(trueLabel).blockAddress;
        final var falseAddress = irContext.get(falseLabel).blockAddress;
        return LLVMBuildCondBr(address, condition, trueAddress, falseAddress);
    }

    public long br(final String name) {
        return LLVMBuildBr(address, irContext.get(name).blockAddress);
    }

    public PhiBuilder phi() {
        return new PhiBuilder(address, irContext);
    }

    public IndirectBrBuilder indirectBr() {
        return new IndirectBrBuilder(address);
    }

    public long select(final long condition, final long trueValue, final long falseValue) {
        return LLVMBuildSelect(address, condition, trueValue, falseValue, "");
    }

    public long call(final long address, final long... args) {
        try (final var stack = MemoryStack.stackPush()) {
            final var typeAddress = LLVMGetElementType(address); // Get function type from pointer
            return LLVMBuildCall2(this.address, typeAddress, address, stack.pointers(args), "");
        }
    }

    public long call(final Function function, final long... args) {
        try (final var stack = MemoryStack.stackPush()) {
            final var fnAddress = function.emit(irContext.getCompileContext(), irContext.getModule(), targetMachine);
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

    public long unreachable() {
        return LLVMBuildUnreachable(address);
    }

    public long vaArg(final long ptr, final long type) {
        return LLVMBuildVAArg(address, ptr, type, "");
    }

    // Intrinsics

    public void trap() {
        call(IntrinsicFunction.TRAP);
    }

    public void debugTrap() {
        call(IntrinsicFunction.DEBUG_TRAP);
    }

    public long returnAddress(final int level) {
        try (final var stack = MemoryStack.stackPush()) {
            final var levelBuffer = stack.mallocInt(1);
            levelBuffer.put(level);
            return call(IntrinsicFunction.RETURN_ADDRESS, MemoryUtil.memAddress(levelBuffer));
        }
    }

    public long addressOfReturnAddress() {
        return call(IntrinsicFunction.ADDRESS_OF_RETURN_ADDRESS);
    }

    public long frameAddress(final int level) {
        try (final var stack = MemoryStack.stackPush()) {
            final var levelBuffer = stack.mallocInt(1);
            levelBuffer.put(level);
            return call(IntrinsicFunction.FRAME_ADDRESS, MemoryUtil.memAddress(levelBuffer));
        }
    }

    public void stackSave(final long ptr) {
        call(IntrinsicFunction.STACK_SAVE, ptr);
    }

    public void stackRestore(final long ptr) {
        call(IntrinsicFunction.STACK_RESTORE, ptr);
    }

    public long threadPointer() {
        return call(IntrinsicFunction.THREAD_POINTER);
    }

    public long sponEntry() {
        return call(IntrinsicFunction.SPON_ENTRY);
    }

    public void prefetch(final long ptr, final int rw, final int locality, final int cacheType) {
        call(IntrinsicFunction.PREFETCH, ptr, rw, locality, cacheType);
    }

    public void vaStart(final long ptr) {
        call(IntrinsicFunction.VA_START, ptr);
    }

    public void vaEnd(final long ptr) {
        call(IntrinsicFunction.VA_END, ptr);
    }

    public void vaCopy(final long srcPtr, final long dstPtr) {
        call(IntrinsicFunction.VA_COPY, srcPtr, dstPtr);
    }

    // Constants

    public long constBool(final boolean value) {
        return LLVMCore.LLVMConstInt(BoolType.INSTANCE.materialize(targetMachine), value ? 1 : 0, false);
    }

    // Comparison

    public long compareInt(final long left, final long right, final EnumCompareOp op, final boolean signed) {
        return LLVMCore.LLVMBuildICmp(this.address, op.asInt(signed), right, left, "");
    }

    public long compareDecimal(final long left, final long right, final EnumCompareOp op, final boolean signed) {
        return LLVMCore.LLVMBuildFCmp(this.address, op.asInt(signed), right, left, "");
    }

    // Non-instruction related functions

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

    @Override
    public void close() {
        dispose();
    }
}

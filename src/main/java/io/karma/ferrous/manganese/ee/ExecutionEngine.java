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

package io.karma.ferrous.manganese.ee;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.llvm.LLVMUtils;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;
import java.util.HashSet;

import static org.lwjgl.llvm.LLVMExecutionEngine.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 11/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ExecutionEngine {
    private final HashSet<GenericValue> values = new HashSet<>();
    private final Module module;
    private final TargetMachine targetMachine;
    private final long address;
    private final CompileContext compileContext = new CompileContext();
    private boolean isDisposed;

    public ExecutionEngine(final Module module, final TargetMachine targetMachine) {
        this.module = module;
        this.targetMachine = targetMachine;
        try (final var stack = MemoryStack.stackPush()) {
            final var addressBuffer = stack.callocPointer(1);
            final var messageBuffer = stack.callocPointer(1);
            if (LLVMCreateExecutionEngineForModule(addressBuffer, module.getAddress(), messageBuffer)) {
                LLVMUtils.checkStatus(messageBuffer);
            }
            address = addressBuffer.get();
        }
    }

    public VoidValue makeVoid() { // Just a convenience/consistency delegate
        return VoidValue.INSTANCE;
    }

    public IntValue makeInt(final BuiltinType type, final long value) {
        final var val = new IntValue(targetMachine, type, value);
        values.add(val);
        return val;
    }

    public long getInt(final GenericValue value) {
        if (!(value instanceof IntValue intValue)) {
            return 0;
        }
        return LLVMGenericValueToInt(intValue.getAddress(), intValue.isSigned());
    }

    public RealValue makeReal(final BuiltinType type, final double value) {
        final var val = new RealValue(targetMachine, type, value);
        values.add(val);
        return val;
    }

    public double getReal(final GenericValue value) {
        if (!(value instanceof RealValue)) {
            return 0;
        }
        final var typeAddress = value.getType().materialize(targetMachine);
        return LLVMGenericValueToFloat(typeAddress, value.getAddress());
    }

    public PointerValue makePointer(final Type type, final long value) {
        final var val = new PointerValue(type, value);
        values.add(val);
        return val;
    }

    public long getPointer(final GenericValue value) {
        if (!(value instanceof PointerValue)) {
            return NULL;
        }
        return LLVMGenericValueToPointer(value.getAddress());
    }

    public GenericValue eval(final Function function, final GenericValue... args) {
        final var returnType = function.getType().getReturnType();
        final var isBuiltin = returnType.isBuiltin();
        if (!isBuiltin && !returnType.isPointer()) {
            throw new IllegalArgumentException("Function return type must be builtin or pointer");
        }
        try (final var stack = MemoryStack.stackPush()) {
            final var fnAddress = function.materialize(compileContext, module, targetMachine);
            final var argValues = Arrays.stream(args).mapToLong(GenericValue::getAddress).toArray();
            final var returnValue = LLVMRunFunction(address, fnAddress, stack.pointers(argValues));
            if (returnType instanceof BuiltinType builtinType) {
                return switch (builtinType) {
                    case I8, I16, I32, I64, ISIZE, U8, U16, U32, U64, USIZE -> new IntValue(builtinType, returnValue);
                    case F32, F64 -> new RealValue(builtinType, returnValue);
                    default -> throw new IllegalStateException("Unreachable");
                };
            }
            else {
                return new PointerValue(returnValue);
            }
        }
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        for (final var value : values) {
            value.dispose();
        }
        LLVMDisposeExecutionEngine(address);
        compileContext.dispose();
        isDisposed = true;
    }

    public long getAddress() {
        return address;
    }
}

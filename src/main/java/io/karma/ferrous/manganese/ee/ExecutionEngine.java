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
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.function.CallingConvention;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.FunctionModifier;
import io.karma.ferrous.manganese.ocm.ir.FunctionIRContext;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import static org.lwjgl.llvm.LLVMExecutionEngine.*;

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

    public BoolValue makeBool(final boolean value) {
        final var val = new BoolValue(targetMachine, value);
        values.add(val);
        return val;
    }

    public boolean getBool(final GenericValue value) {
        if (!(value instanceof BoolValue)) {
            throw new IllegalArgumentException("Invalid value type");
        }
        return LLVMGenericValueToInt(value.getAddress(), true) == 1;
    }

    public CharValue makeChar(final char value) {
        final var val = new CharValue(targetMachine, value);
        values.add(val);
        return val;
    }

    public char getChar(final GenericValue value) {
        if (!(value instanceof CharValue)) {
            throw new IllegalArgumentException("Invalid value type");
        }
        return (char) LLVMGenericValueToInt(value.getAddress(), true);
    }

    public IntValue makeInt(final Type type, final long value) {
        final var val = new IntValue(targetMachine, type, value);
        values.add(val);
        return val;
    }

    public long getInt(final GenericValue value) {
        if (!(value instanceof IntValue intValue)) {
            throw new IllegalArgumentException("Invalid value type");
        }
        return LLVMGenericValueToInt(intValue.getAddress(), intValue.isSigned());
    }

    public RealValue makeReal(final Type type, final double value) {
        final var val = new RealValue(targetMachine, type, value);
        values.add(val);
        return val;
    }

    public double getReal(final GenericValue value) {
        if (!(value instanceof RealValue)) {
            throw new IllegalArgumentException("Invalid value type");
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
            throw new IllegalArgumentException("Invalid value type");
        }
        return LLVMGenericValueToPointer(value.getAddress());
    }

    public GenericValue eval(final Function function, final GenericValue... args) {
        final var returnType = function.getType().getReturnType();
        try (final var stack = MemoryStack.stackPush()) {
            final var fnAddress = function.emit(compileContext, module, targetMachine);
            final var argValues = Arrays.stream(args).mapToLong(GenericValue::getAddress).toArray();
            final var returnValue = LLVMRunFunction(address, fnAddress, stack.pointers(argValues));
            final var kind = returnType.getKind();
            if (kind.isBuiltin()) {
                return switch (kind) {
                    case INT, UINT -> new IntValue(returnType, returnValue);
                    case REAL -> new RealValue(returnType, returnValue);
                    case BOOL -> new BoolValue(returnValue);
                    case VOID -> VoidValue.INSTANCE;
                    default -> throw new IllegalStateException("Unsupported return type");
                };
            }
            else {
                return new PointerValue(returnValue); // Pointers (and refs)
            }
        }
    }

    /**
     * Wrapper around {@link #eval(Function, GenericValue...)} that creates a temporary
     * function which gets removed from the module after evaluation. This is done to
     * prevent another module from being allocated.
     *
     * @param expression The expression to evaluate.
     * @return The result of the expression evaluation.
     */
    public GenericValue eval(final Expression expression) {
        final var functionName = new Identifier(STR."eval\{expression.hashCode()}");
        final var tokenSlice = expression.getTokenSlice();
        final var functionType = Types.function(expression.getType(targetMachine),
            Collections.emptyList(),
            false,
            Functions.castingIdentity(),
            tokenSlice);
        final var function = new Function(functionName,
            CallingConvention.CDECL,
            functionType,
            EnumSet.noneOf(FunctionModifier.class),
            tokenSlice,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
        final var irContext = new FunctionIRContext(compileContext, module, targetMachine, function);
        new ReturnStatement(expression, tokenSlice).emit(targetMachine, irContext); // Return expression as value
        final var result = eval(function);
        irContext.drop(); // Drop basic block appended to module
        function.delete(); // Delete function from module
        return result;
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

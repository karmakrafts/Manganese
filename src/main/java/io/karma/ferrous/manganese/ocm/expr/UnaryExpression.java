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

package io.karma.ferrous.manganese.ocm.expr;

import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.function.FunctionReference;
import io.karma.ferrous.manganese.ocm.ir.IRBuilder;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeKind;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Operator;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static org.lwjgl.llvm.LLVMCore.LLVMConstInt;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class UnaryExpression implements Expression {
    private final Operator op;
    private final Expression value;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;

    public UnaryExpression(final Operator op, final Expression value, final TokenSlice tokenSlice) {
        if (!op.isUnary()) {
            throw new IllegalArgumentException(String.format("%s is not a unary operator", op));
        }
        this.op = op;
        this.value = value;
        this.tokenSlice = tokenSlice;
    }

    private long emitNegate(final long address, final TargetMachine targetMachine, final IRContext irContext,
                            final Type type) {
        final var builder = irContext.getCurrentOrCreate();
        final var zero = type.makeDefaultValue(targetMachine).emit(targetMachine, irContext);
        if (type.getKind() == TypeKind.REAL) {
            return builder.fsub(zero, address);
        }
        return builder.sub(zero, address);
    }

    private long emitPreIncrement(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitIncrement(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitPreDecrement(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitDecrement(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitPreInverseAssign(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitInverseAssign(final long value, final IRBuilder builder) {
        return value;
    }

    private long emitReference(final TargetMachine targetMachine, final IRContext irContext, final IRBuilder builder) {
        if (!(value instanceof ReferenceExpression refExpr)) {
            return NULL; // TODO: check/report error
        }
        return switch(refExpr.getReference()) { // @formatter:off
            case FunctionReference fnRef -> {
                final var function = fnRef.get();
                if(function == null) {
                    yield NULL; // TODO: check/report error
                }
                yield function.materialize(irContext.getModule(), targetMachine);
            }
            case ValueStorage storage    -> storage.getAddress(targetMachine, irContext);
            default                      -> throw new IllegalStateException("Unsupported reference type");
        }; // @formatter:on
    }

    private long emitDereference(final IRBuilder builder) {
        return NULL;
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Expression

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        var type = value.getType();
        switch(op) { // @formatter:off
            case REF:   return emitReference(targetMachine, irContext, builder);
            case DEREF: return emitDereference(builder);
        } // @formatter:on
        if (type.isReference()) {
            type = type.getBaseType();
        }
        if (!type.getKind().isBuiltin()) {
            return NULL; // TODO: implement user defined operator calls
        }
        final var address = value.emit(targetMachine, irContext);
        return switch(op) { // @formatter:off
            case MINUS, PLUS    -> emitNegate(address, targetMachine, irContext, type);
            case INV            -> builder.xor(address, LLVMConstInt(type.materialize(targetMachine), -1, false));
            case NOT            -> builder.xor(address, LLVMConstInt(type.materialize(targetMachine), 1, false));
            case PRE_INC        -> emitPreIncrement(address, builder);
            case INC            -> emitIncrement(address, builder);
            case PRE_DEC        -> emitPreDecrement(address, builder);
            case DEC            -> emitDecrement(address, builder);
            case PRE_INV_ASSIGN -> emitPreInverseAssign(address, builder);
            case INV_ASSIGN     -> emitInverseAssign(address, builder);
            default             -> throw new IllegalStateException("Operator not supported");
        }; // @formatter:on
    }

    @Override
    public Type getType() {
        return value.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UnaryExpression expr) {
            return op == expr.op && value.equals(expr.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s", op, value);
    }
}

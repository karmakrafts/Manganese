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

import io.karma.ferrous.manganese.ocm.BlockBuilder;
import io.karma.ferrous.manganese.ocm.BlockContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
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

    private long emitPreIncrement(final long value, final BlockBuilder builder) {
        return value;
    }

    private long emitIncrement(final long value, final BlockBuilder builder) {
        return value;
    }

    private long emitPreDecrement(final long value, final BlockBuilder builder) {
        return value;
    }

    private long emitDecrement(final long value, final BlockBuilder builder) {
        return value;
    }

    private long emitPreInverseAssign(final long value, final BlockBuilder builder) {
        return value;
    }

    private long emitInverseAssign(final long value, final BlockBuilder builder) {
        return value;
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
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        final var type = value.getType();
        if (!(type instanceof BuiltinType builtinType)) {
            return NULL; // TODO: implement user defined operator calls
        }
        final var address = value.emit(targetMachine, blockContext);
        return switch(op) { // @formatter:off
            case MINUS          -> builtinType.isFloatType() ? builder.fneg(address) : builder.neg(address);
            case PLUS           -> builtinType.isFloatType() ? builder.fneg(address) : builder.neg(address);
            case INV            -> builder.xor(address, LLVMConstInt(type.materialize(targetMachine), -1, false));
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

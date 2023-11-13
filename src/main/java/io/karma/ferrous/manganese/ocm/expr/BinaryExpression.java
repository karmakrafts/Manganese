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
import io.karma.ferrous.manganese.ocm.ir.IRBuilder;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Operator;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class BinaryExpression implements Expression {
    private final Operator op;
    private final Expression lhs;
    private final Expression rhs;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;
    private boolean isResultDiscarded;

    public BinaryExpression(final Operator op, final Expression lhs, final Expression rhs,
                            final TokenSlice tokenSlice) {
        if (!op.isBinary()) {
            throw new IllegalArgumentException(String.format("%s is not a binary operator", op));
        }
        this.op = op;
        this.lhs = lhs;
        this.rhs = rhs;
        this.tokenSlice = tokenSlice;
    }

    public Operator getOp() {
        return op;
    }

    public Expression getLHS() {
        return lhs;
    }

    public Expression getRHS() {
        return rhs;
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
    public boolean isResultDiscarded() {
        return isResultDiscarded;
    }

    @Override
    public void setResultDiscarded(final boolean isResultDiscarded) {
        this.isResultDiscarded = isResultDiscarded;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    private long emitAssignment(final TargetMachine targetMachine, final IRContext irContext) {
        if (!(lhs instanceof ReferenceExpression lhsRefExpr)) {
            return NULL;
        }
        lhsRefExpr.setIsWrite(true); // This is always a write to the left side of the expression
        return switch (lhsRefExpr.getReference()) {
            case ValueStorage storage -> {
                storage.store(rhs, targetMachine, irContext);
                if (isResultDiscarded) {
                    yield NULL; // Omit
                }
                yield storage.load(targetMachine, irContext);
            }
            default -> NULL;
        };
    }

    private long emitSwap(final TargetMachine targetMachine, final IRContext irContext) {
        if (!(lhs instanceof ReferenceExpression lhsRef) || !(rhs instanceof ReferenceExpression rhsRef)) {
            return NULL;
        }
        lhsRef.setIsWrite(true);
        rhsRef.setIsWrite(true);
        return switch (lhsRef.getReference()) {
            case ValueStorage lhsStorage -> switch (rhsRef.getReference()) {
                case ValueStorage rhsStorage -> {
                    final var rhsAddress = rhsStorage.getAddress(targetMachine, irContext);
                    // TODO: re-implement this!
                    yield rhsAddress;
                }
                default -> NULL;
            };
            default -> NULL;
        };
    }

    private long emitDiv(final long lhs, final long rhs, final BuiltinType builtinType, final IRBuilder builder) {
        if (builtinType.isFloatType()) {
            return builder.fdiv(lhs, rhs);
        }
        if (builtinType.isUnsignedInt()) {
            return builder.udiv(lhs, rhs);
        }
        return builder.sdiv(lhs, rhs);
    }

    private long emitMod(final long lhs, final long rhs, final BuiltinType builtinType, final IRBuilder builder) {
        if (builtinType.isFloatType()) {
            return builder.frem(lhs, rhs);
        }
        if (builtinType.isUnsignedInt()) {
            return builder.urem(lhs, rhs);
        }
        return builder.srem(lhs, rhs);
    }

    private boolean needsLoadDedup() {
        if (!(lhs instanceof ReferenceExpression lhsRef) || !(rhs instanceof ReferenceExpression rhsRef)) {
            return false;
        }
        return lhsRef.getReference() == rhsRef.getReference();
    }

    private long emitBuiltin(final TargetMachine targetMachine, final IRContext irContext,
                             final BuiltinType builtinType) {
        final var builder = irContext.getCurrentOrCreate();
        final var lhs = this.lhs.emit(targetMachine, irContext);
        final var rhs = needsLoadDedup() ? lhs : this.rhs.emit(targetMachine, irContext);
        return switch (op) { // @formatter:off
            case PLUS        -> builtinType.isFloatType() ? builder.fadd(lhs, rhs) : builder.add(lhs, rhs);
            case MINUS       -> builtinType.isFloatType() ? builder.fsub(lhs, rhs) : builder.sub(lhs, rhs);
            case TIMES       -> builtinType.isFloatType() ? builder.fmul(lhs, rhs) : builder.mul(lhs, rhs);
            case DIV         -> emitDiv(lhs, rhs, builtinType, builder);
            case MOD         -> emitMod(lhs, rhs, builtinType, builder);
            case AND         -> builder.and(lhs, rhs);
            case OR          -> builder.or(lhs, rhs);
            case XOR         -> builder.xor(lhs, rhs);
            case SHL         -> builder.shl(lhs, rhs);
            case SHR         -> builtinType.isUnsignedInt() ? builder.lshr(lhs, rhs) : builder.ashr(lhs, rhs);
            default          -> throw new IllegalStateException("Unsupported operator");
        }; // @formatter:on
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        if (op == Operator.ASSIGN) {
            return emitAssignment(targetMachine, irContext);
        }
        if (op == Operator.SWAP) {
            return emitSwap(targetMachine, irContext);
        }
        final var rhsType = rhs.getType();
        if (!(rhsType instanceof BuiltinType builtinType)) {
            throw new UnsupportedOperationException();
        }
        return emitBuiltin(targetMachine, irContext, builtinType);
    }

    @Override
    public Type getType() {
        return lhs.getType(); // TODO: add type resolution for overloaded operator function calls
    }

    @Override
    public int hashCode() {
        return Objects.hash(op, lhs, rhs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BinaryExpression expr) {
            return op == expr.op && lhs.equals(expr.lhs) && rhs.equals(expr.rhs);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s%s", lhs, op, rhs);
    }
}

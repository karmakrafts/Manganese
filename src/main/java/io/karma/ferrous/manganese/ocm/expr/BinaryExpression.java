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
import io.karma.ferrous.manganese.ocm.constant.BoolConstant;
import io.karma.ferrous.manganese.ocm.ir.IRBuilder;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.EnumCompareOp;
import io.karma.ferrous.manganese.util.Operator;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;

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
            throw new IllegalArgumentException(STR."\{op} is not a binary operator");
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
                storage.store(rhs.emit(targetMachine, irContext), targetMachine, irContext);
                storage.setValue(rhs);
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

    private long emitDiv(final long lhs, final long rhs, final Type type, final IRBuilder builder) {
        return switch (type.getKind()) { // @formatter:off
            case REAL -> builder.fdiv(lhs, rhs);
            case UINT -> builder.udiv(lhs, rhs);
            default   -> builder.sdiv(lhs, rhs);
        }; // @formatter:on
    }

    private long emitMod(final long lhs, final long rhs, final Type type, final IRBuilder builder) {
        return switch (type.getKind()) { // @formatter:off
            case REAL -> builder.frem(lhs, rhs);
            case UINT -> builder.urem(lhs, rhs);
            default   -> builder.srem(lhs, rhs);
        }; // @formatter:on
    }

    private long emitComparison(EnumCompareOp op, final long lhs, final long rhs, final Type type, final IRBuilder builder) {
        if (type.isBuiltin()) {
            if (type instanceof IntType intType)
                return builder.compareInt(lhs, rhs, op, !intType.isUnsigned());
            else if (type instanceof SizeType sizeType)
                return builder.compareInt(lhs, rhs, op, !sizeType.isUnsigned());
            else if (type instanceof RealType)
                return builder.compareDecimal(lhs, rhs, op, true);
            return NULL;
        } else {
            // TODO: Implement compare
            return builder.constBool(false);
        }
    }

    private boolean needsLoadDedup() {
        if (!(lhs instanceof ReferenceExpression lhsRef) || !(rhs instanceof ReferenceExpression rhsRef)) {
            return false;
        }
        return lhsRef.getReference() == rhsRef.getReference();
    }

    private long emitBuiltin(final TargetMachine targetMachine, final IRContext irContext,
                             final Type type) {
        final var kind = type.getKind();
        final var builder = irContext.getCurrentOrCreate();
        final var lhs = this.lhs.emit(targetMachine, irContext);
        final var rhs = needsLoadDedup() ? lhs : this.rhs.emit(targetMachine, irContext);
        return switch (op) { // @formatter:off
            case PLUS    -> kind == TypeKind.REAL ? builder.fadd(lhs, rhs) : builder.add(lhs, rhs);
            case MINUS   -> kind == TypeKind.REAL ? builder.fsub(lhs, rhs) : builder.sub(lhs, rhs);
            case TIMES   -> kind == TypeKind.REAL ? builder.fmul(lhs, rhs) : builder.mul(lhs, rhs);
            case DIV     -> emitDiv(lhs, rhs, type, builder);
            case MOD     -> emitMod(lhs, rhs, type, builder);
            case CMP_GTH -> emitComparison(EnumCompareOp.GREATER_THAN, lhs, rhs, type, builder);
            case CMP_GEQ -> emitComparison(EnumCompareOp.GREATER_THAN_EQUAL, lhs, rhs, type, builder);
            case CMP_LTH -> emitComparison(EnumCompareOp.LESS_THAN, lhs, rhs, type, builder);
            case CMP_LEQ -> emitComparison(EnumCompareOp.LESS_THAN_EQUAL, lhs, rhs, type, builder);
            case EQ      -> emitComparison(EnumCompareOp.EQUALS, lhs, rhs, type, builder);
            case NEQ     -> emitComparison(EnumCompareOp.NOT_EQUALS, lhs, rhs, type, builder);
            case AND     -> builder.and(lhs, rhs);
            case OR      -> builder.or(lhs, rhs);
            case XOR     -> builder.xor(lhs, rhs);
            case SHL     -> builder.shl(lhs, rhs);
            case SHR     -> kind == TypeKind.UINT ? builder.lshr(lhs, rhs) : builder.ashr(lhs, rhs);
            default      -> throw new IllegalStateException("Unsupported operator");
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
        var lhsType = lhs.getType(targetMachine);
        if (lhsType != null && lhsType.isRef()) {
            lhsType = lhsType.getBaseType();
        }
        if (lhsType == null || !lhsType.getKind().isBuiltin()) {
            throw new UnsupportedOperationException();
        }
        return emitBuiltin(targetMachine, irContext, lhsType);
    }

    @Override
    public Type getType(final TargetMachine targetMachine) {
        return lhs.getType(targetMachine); // TODO: add type resolution for overloaded operator function calls
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
        return STR."\{lhs}\{op}\{rhs}";
    }
}

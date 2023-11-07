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

import io.karma.ferrous.manganese.ocm.BlockContext;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Operator;
import io.karma.ferrous.manganese.util.TokenSlice;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class BinaryExpression implements Expression {
    private final Operator op;
    private final Expression lhs;
    private final Expression rhs;
    private final TokenSlice tokenSlice;

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

    @Override
    public TokenSlice tokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        final var lhsType = this.lhs.getType();
        if (!(lhsType instanceof BuiltinType builtinType)) {
            return 0L; // TODO: implement user defined operator calls
        }
        final var lhs = this.lhs.emit(targetMachine, blockContext);
        final var rhs = this.rhs.emit(targetMachine, blockContext);
        return switch (op) { // @formatter:off
            case PLUS  -> builtinType.isFloatType() ? builder.fadd(lhs, rhs) : builder.add(lhs, rhs);
            case MINUS -> builtinType.isFloatType() ? builder.fsub(lhs, rhs) : builder.sub(lhs, rhs);
            case TIMES -> builtinType.isFloatType() ? builder.fmul(lhs, rhs) : builder.mul(lhs, rhs);
            case DIV   -> {
                if(builtinType.isFloatType()) {
                    yield builder.fdiv(lhs, rhs);
                }
                if(builtinType.isUnsignedInt()) {
                    yield builder.udiv(lhs, rhs);
                }
                yield builder.sdiv(lhs, rhs);
            }
            case MOD   -> {
                if(builtinType.isFloatType()) {
                    yield builder.frem(lhs, rhs);
                }
                if(builtinType.isUnsignedInt()) {
                    yield builder.urem(lhs, rhs);
                }
                yield builder.srem(lhs, rhs);
            }
            default    -> throw new IllegalStateException("Unsupported operator");
        }; // @formatter:on
    }

    @Override
    public Type getType() {
        return lhs.getType(); // We always use the type of the left hand side expression
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

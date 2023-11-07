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

package io.karma.ferrous.manganese.ocm.statement;

import io.karma.ferrous.manganese.ocm.BlockContext;
import io.karma.ferrous.manganese.ocm.constant.VoidConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class ReturnStatement implements Statement {
    private final Expression value;
    private final TokenSlice tokenSlice;

    public ReturnStatement(final Expression value, final TokenSlice tokenSlice) {
        this.value = value;
        this.tokenSlice = tokenSlice;
    }

    public ReturnStatement(final TokenSlice tokenSlice) {
        this(VoidConstant.INSTANCE, tokenSlice);
    }

    public Expression getValue() {
        return value;
    }

    // Statement

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        final var type = value.getType();
        if (type.isImaginary()) {
            return 0L; // We don't emit anything for imaginary types
        }
        if (type == BuiltinType.VOID) {
            return builder.ret();
        }
        return builder.ret(value.emit(targetMachine, blockContext));
    }

    @Override
    public boolean returnsFromCurrentScope() {
        return true;
    }

    // Object

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReturnStatement statement) {
            return value.equals(statement.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("return %s", value);
    }
}

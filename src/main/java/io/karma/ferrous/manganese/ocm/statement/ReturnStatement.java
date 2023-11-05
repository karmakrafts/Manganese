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

import io.karma.ferrous.manganese.ocm.BlockBuilder;
import io.karma.ferrous.manganese.ocm.constant.VoidConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.target.TargetMachine;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class ReturnStatement implements Statement {
    private final Expression value;

    public ReturnStatement(final Expression value) {
        this.value = value;
    }

    public ReturnStatement() {
        this(VoidConstant.INSTANCE);
    }

    public Expression getValue() {
        return value;
    }

    // Statement

    @Override
    public void emit(final TargetMachine targetMachine, final BlockBuilder builder) {

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

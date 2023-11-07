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
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class CallExpression implements Expression {
    private final Function function;
    private final Expression[] args;
    private final TokenSlice tokenSlice;

    public CallExpression(final Function function, final TokenSlice tokenSlice, final Expression... args) {
        this.function = function;
        this.tokenSlice = tokenSlice;
        this.args = args;
    }

    public Function getFunction() {
        return function;
    }

    public Expression[] getArgs() {
        return args;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public Type getType() {
        return function.getType().getReturnType();
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        final var args = Arrays.stream(this.args).mapToLong(expr -> expr.emit(targetMachine, blockContext)).toArray();
        return builder.call(function, args);
    }
}

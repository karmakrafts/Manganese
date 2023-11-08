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
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class LetExpression implements Expression {
    private final String name;
    private final Type type;
    private final TokenSlice tokenSlice;
    private Expression value;
    private Scope enclosingScope;

    public LetExpression(final String name, final Type type, final @Nullable Expression value,
                         final TokenSlice tokenSlice) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.tokenSlice = tokenSlice;
    }

    public LetExpression(final String name, final Expression value, final TokenSlice tokenSlice) {
        this(name, value.getType(), value, tokenSlice);
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(final @Nullable Expression value) {
        this.value = value;
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
    public Type getType() {
        return type;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final BlockContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        final var address = builder.alloca(type.materialize(targetMachine));
        if (value != null) {
            builder.store(value.emit(targetMachine, blockContext), address);
        }
        return address;
    }
}

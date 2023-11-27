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

import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.lazy.Lazy;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ScopeExpression implements Expression, Scope {
    private final ScopeType scopeType;
    private final Statement[] statements;
    private final Lazy<Type> type = new Lazy<>(this::findReturnType);
    private final TokenSlice tokenSlice;
    private final UUID uuid = UUID.randomUUID();
    private Scope enclosingScope;

    public ScopeExpression(final ScopeType scopeType, final TokenSlice tokenSlice, final Statement... statements) {
        this.scopeType = scopeType;
        this.statements = statements;
        this.tokenSlice = tokenSlice;
    }

    public Statement[] getStatements() {
        return statements;
    }

    private Type findReturnType() {
        final var types = new ArrayList<Type>();
        for (final var statement : statements) {
            if (!(statement instanceof ReturnStatement returnStatement)) {
                continue;
            }
            final var type = returnStatement.getValue().getType();
            if (type == BuiltinType.VOID) {
                continue;
            }
            types.add(type);
        }
        return types.isEmpty() ? BuiltinType.VOID : Types.findCommonType(types);
    }

    public UUID getUUID() {
        return uuid;
    }

    // Scope

    @Override
    public ScopeType getScopeType() {
        return scopeType;
    }

    @Override
    public Identifier getName() {
        return new Identifier(String.format("scope%s", uuid));
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
    public Type getType() {
        return type.getOrCreate();
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        for (final var statement : statements) {
            statement.emit(targetMachine, irContext);
        }
        return 0;
    }
}

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

import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public abstract class AbstractScopeExpression implements Expression, Scope {
    protected final ArrayList<Statement> statements = new ArrayList<>();
    protected final String name;
    protected final Identifier scopeName;
    protected final TokenSlice tokenSlice;
    protected Scope enclosingScope;

    protected AbstractScopeExpression(final String scopeName, final TokenSlice tokenSlice) {
        name = scopeName;
        this.scopeName = new Identifier(scopeName);
        this.tokenSlice = tokenSlice;
    }

    protected AbstractScopeExpression(final TokenSlice tokenSlice) {
        this(String.format("scope%s", UUID.randomUUID()), tokenSlice);
    }

    public void addStatement(final Statement statement) {
        if (statements.contains(statement)) {
            return;
        }
        statements.add(statement);
    }

    // Expression

    @Override
    public Type getType(final TargetMachine targetMachine) {
        // @formatter:off
        return Types.findCommonType(targetMachine, statements.stream()
            .filter(Expression.class::isInstance)
            .map(Expression.class::cast)
            .map(expr -> expr.getType(targetMachine))
            .toList());
        // @formatter:on
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    // Scope

    @Override
    public Identifier getName() {
        return scopeName;
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
}

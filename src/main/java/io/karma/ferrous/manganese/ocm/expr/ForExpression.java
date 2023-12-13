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
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.statement.YieldStatement;
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
 * @since 09/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class ForExpression implements Expression, Scope {
    private final LetStatement variable;
    private final Expression condition;
    private final Statement action;
    private final Expression defaultValue;
    private final Identifier scopeName;
    private final TokenSlice tokenSlice;
    private final ArrayList<Statement> statements = new ArrayList<>();
    private Scope enclosingScope;

    public ForExpression(final LetStatement variable, final Expression condition, final Statement action,
                         final @Nullable Expression defaultValue, final @Nullable Identifier scopeName,
                         final TokenSlice tokenSlice) {
        this.variable = variable;
        this.condition = condition;
        this.action = action;
        this.defaultValue = defaultValue;
        // @formatter:off
        this.scopeName = scopeName == null
            ? new Identifier(STR."for\{UUID.randomUUID()}")
            : scopeName;
        // @formatter:on
        this.tokenSlice = tokenSlice;
    }

    // Scope

    @Override
    public Identifier getName() {
        return scopeName;
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.FOR;
    }

    // Expression

    @Override
    public Type getType(final TargetMachine targetMachine) {
        final var types = new ArrayList<Type>();
        if (defaultValue != null) {
            types.add(defaultValue.getType(targetMachine));
        }
        for (final var statement : statements) {
            if (!(statement instanceof YieldStatement yieldStatement)) {
                continue;
            }
            types.add(yieldStatement.getValue().getType(targetMachine));
        }
        return Types.findCommonType(targetMachine, types);
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var internalName = scopeName.toInternalName();
        final var condLabel = STR."\{internalName}.cond";
        final var bodyLabel = STR."\{internalName}.body";
        final var exitLabel = STR."\{internalName}.exit";

        final var condBuilder = irContext.getAndPush(condLabel);
        final var condAddress = condition.emit(targetMachine, irContext);
        condBuilder.condBr(condAddress, bodyLabel, exitLabel);
        irContext.popCurrent();

        final var bodyBuilder = irContext.getAndPush(bodyLabel);
        for (final var statement : statements) {
            statement.emit(targetMachine, irContext);
        }
        bodyBuilder.br(condLabel);
        irContext.popCurrent();

        irContext.getAndPush(exitLabel); // This is the new default block
        return 0L;
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }
}

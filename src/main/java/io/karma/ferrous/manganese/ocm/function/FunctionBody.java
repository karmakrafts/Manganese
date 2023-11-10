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

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.ir.DefaultIRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionBody implements Scope {
    private final Function function;
    private final ArrayList<Statement> statements;
    private boolean isAppended;
    private Scope enclosingScope;

    public FunctionBody(final Function function, final Statement... statements) {
        this.function = function;
        this.statements = new ArrayList<>(Arrays.asList(statements));
    }

    public Function getFunction() {
        return function;
    }

    public ArrayList<Statement> getStatements() {
        return statements;
    }

    public void append(final CompileContext compileContext, final Module module, final TargetMachine targetMachine) {
        if (isAppended) {
            return;
        }
        final var context = new DefaultIRContext(compileContext, module, targetMachine, function);
        for (final var statement : statements) {
            statement.emit(targetMachine, context);
        }
        context.dispose();
        isAppended = true;
    }

    // Scope

    @Override
    public Identifier getName() {
        return function.getName();
    }

    @Override
    public ScopeType getType() {
        return ScopeType.FUNCTION;
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }
}

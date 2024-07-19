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
import io.karma.ferrous.manganese.ocm.ir.FunctionIRContext;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.ocm.statement.LabeledStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionBody implements Scope {
    private final Function function;
    private final List<Statement> statements;
    private boolean isAppended;

    public FunctionBody(final Function function, final List<Statement> statements) {
        this.function = function;
        this.statements = statements;
    }

    public Function getFunction() {
        return function;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void append(final CompileContext compileContext, final Module module, final TargetMachine targetMachine) {
        if (isAppended) {
            return;
        }
        try (final var context = new FunctionIRContext(compileContext, module, targetMachine, function)) {
            //context.reset();
            //for (final var statement : statements) {
            //    if (!(statement instanceof LabeledStatement labeled)) {
            //        continue;
            //    }
            //    labeled.emit(targetMachine, context); // Pre-emit all blocks in the right order
            //}
            context.reset();
            var isTerminated = false;
            var lastBlock = IRContext.DEFAULT_BLOCK;
            for (final var statement : statements) {
                if (statement instanceof LabeledStatement labeled) {
                    if (!lastBlock.equals(labeled.getLabelName())) {
                        isTerminated = false;
                    }
                }
                if (isTerminated) {
                    continue; // Skip as long as the block is terminated
                }
                statement.emit(targetMachine, context);
                if (statement.terminatesBlock()) {
                    isTerminated = true;
                }
            }
        }
        isAppended = true;
    }

    // Scope

    @Override
    public Identifier getName() {
        return function.getName();
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.FUNCTION;
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return function.getEnclosingScope();
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }
}

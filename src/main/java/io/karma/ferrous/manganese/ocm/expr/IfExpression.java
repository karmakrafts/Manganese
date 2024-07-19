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
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.VoidType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class IfExpression implements Expression, Scope {
    private final List<Pair<Expression, ScopeExpression>> branches;
    private final TokenSlice tokenSlice;
    private final Identifier entryLabel;
    private Scope enclosingScope;

    public IfExpression(final List<Pair<Expression, ScopeExpression>> branches, final TokenSlice tokenSlice) {
        if (branches.isEmpty()) {
            throw new IllegalArgumentException("If expression requires at least one block");
        }

        this.entryLabel = new Identifier(STR."if\{UUID.randomUUID()}");
        this.branches = branches;
        this.tokenSlice = tokenSlice;
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
    public Type getType(final TargetMachine targetMachine) {
        return Types.findCommonType(targetMachine, branches.stream().map(pair -> {
            final var statement = pair.getRight();
            if (statement instanceof Expression expr) {
                return expr.getType(targetMachine);
            }
            return VoidType.INSTANCE;
        }).toList());
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var entryLabel = this.entryLabel.toInternalName();
        irContext.getCurrentOrCreate().br(STR."selector_\{entryLabel}-0");

        // Implement branches
        for (int i = 0; i < this.branches.size(); i++) {
            final var branch = this.branches.get(i);
            final var codeBranchLabel = STR."code_\{entryLabel}-\{i}";

            // Generate selector branch for code branch
            final var selectorBuilder = irContext.getAndPush(STR."selector_\{entryLabel}-\{i}");
            final var falseLabel = i != this.branches.size() - 1 ? STR."selector_\{entryLabel}-\{i + 1}" : STR."end_\{entryLabel}";
            if (branch.getLeft() != null) {
                selectorBuilder.condBr(branch.getLeft().emit(targetMachine, irContext), codeBranchLabel, falseLabel);
            }
            else {
                selectorBuilder.br(codeBranchLabel);
            }
            irContext.popCurrent();

            // Generate code branch for execution
            final var codeBuilder = irContext.getAndPush(codeBranchLabel);
            branch.getRight().emit(targetMachine, irContext);
            if (!irContext.isScopeTerminated()) {
                codeBuilder.br(STR."end_\{entryLabel}");
            }
            irContext.popCurrent();
        }

        irContext.getAndPush(STR."end_\{entryLabel}");
        return NULL; // Return value ref to result register TODO: If as expression
    }

    @Override
    public Identifier getName() {
        return this.entryLabel;
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.IF;
    }

}

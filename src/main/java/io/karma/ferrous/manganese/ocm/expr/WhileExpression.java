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
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Cedric Hammes, Cach30verfl0w
 * @since 23/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class WhileExpression extends AbstractScopeExpression {

    private final Expression condition;
    private final boolean isDoWhile;

    public WhileExpression(final Identifier scopeName, final TokenSlice tokenSlice, final Expression condition,
                           final boolean isDoWhile) {
        super(scopeName.toInternalName(), tokenSlice);
        this.condition = condition;
        this.isDoWhile = isDoWhile;
    }

    public WhileExpression(final TokenSlice tokenSlice, final Expression condition, final boolean isDoWhile) {
        super(tokenSlice);
        this.condition = condition;
        this.isDoWhile = isDoWhile;
    }

    // Expression

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var labelName = this.scopeName.toInternalName();
        final var mainBuilder = irContext.getCurrentOrCreate();

        // Create jump trampoline
        final var trampolineBuilder = irContext.getAndPush(STR."trampoline_\{labelName}");
        trampolineBuilder.condBr(this.condition.emit(targetMachine, irContext), labelName, STR."end_\{labelName}");
        irContext.popCurrent();

        // Conditional jump if not do while, otherwise jump
        if (this.isDoWhile) {
            mainBuilder.br(labelName);
        }
        else {
            mainBuilder.br(STR."trampoline_\{labelName}");
        }

        // Create loop block
        final var loopBlockBuilder = irContext.getAndPush(labelName);
        for (final var statement : this.statements) {
            statement.emit(targetMachine, irContext);
        }
        loopBlockBuilder.br(STR."trampoline_\{labelName}");
        irContext.popCurrent();

        irContext.getAndPush(STR."end_\{labelName}");
        return NULL; // Return value ref to result register
    }

    // Scope

    @Override
    public ScopeType getScopeType() {
        return ScopeType.WHILE;
    }

}

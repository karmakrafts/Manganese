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
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import java.util.UUID;
import java.util.function.LongSupplier;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Cedric Hammes, Cach30verfl0w
 * @since 24/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class ScopeExpression extends AbstractScopeExpression {
    private final ScopeType type;
    private final LongSupplier valueSupplier;

    public ScopeExpression(final String scopeName, final TokenSlice tokenSlice, final ScopeType scopeType,
                           final LongSupplier valueSupplier) {
        super(scopeName, tokenSlice);
        this.valueSupplier = valueSupplier;
        this.type = scopeType;
    }

    public ScopeExpression(final TokenSlice tokenSlice, final ScopeType scopeType, final LongSupplier valueSupplier) {
        this(STR."scope\{UUID.randomUUID()}", tokenSlice, scopeType, valueSupplier);
    }

    public ScopeExpression(final String scopeName, final TokenSlice tokenSlice, final ScopeType scopeType) {
        this(scopeName, tokenSlice, scopeType, () -> NULL);
    }

    public ScopeExpression(final TokenSlice tokenSlice, final ScopeType scopeType) {
        this(tokenSlice, scopeType, () -> NULL);
    }

    @Override
    public long emit(TargetMachine targetMachine, IRContext irContext) {
        irContext.setScopeTerminated(false);
        for (final var statement : this.statements) {
            if (statement.terminatesBlock()) {
                irContext.setScopeTerminated(true);
            }
            statement.emit(targetMachine, irContext);
        }
        return this.valueSupplier.getAsLong();
    }

    @Override
    public ScopeType getScopeType() {
        return this.type;
    }

}

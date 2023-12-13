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

import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Alexander Hinze
 * @since 09/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class TernaryExpression implements Expression, Named {
    private final Expression condition;
    private final Expression trueValue;
    private final Expression falseValue;
    private final TokenSlice tokenSlice;
    private final Identifier name;
    private Scope enclosingScope;

    public TernaryExpression(final Expression condition, final Expression trueValue, final Expression falseValue,
                             final TokenSlice tokenSlice) {
        this.condition = condition;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
        this.tokenSlice = tokenSlice;
        name = new Identifier(STR."ternary\{UUID.randomUUID()}");
    }

    // Named

    @Override
    public Identifier getName() {
        return name;
    }

    // Expression

    @Override
    public Type getType(final TargetMachine targetMachine) {
        return Types.findCommonType(targetMachine, trueValue.getType(targetMachine), falseValue.getType(targetMachine));
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var condition = this.condition.emit(targetMachine, irContext);
        final var trueValue = this.trueValue.emit(targetMachine, irContext);
        final var falseValue = this.falseValue.emit(targetMachine, irContext);
        return irContext.getCurrentOrCreate().select(condition, trueValue, falseValue);
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

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

package io.karma.ferrous.manganese.ocm.statement;

import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 09/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class PanicStatement implements Statement {
    private final Expression value;
    private final boolean isConst;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;

    public PanicStatement(final Expression value, final boolean isConst, final TokenSlice tokenSlice) {
        this.value = value;
        this.tokenSlice = tokenSlice;
        this.isConst = isConst;
    }

    public Expression getValue() {
        return value;
    }

    public boolean isConst() {
        return isConst;
    }

    // Statement

    @Override
    public boolean terminatesScope() {
        return true;
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

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        if (isConst) {
            // TODO: implement const-evaluation of string expression
            irContext.getCompileContext().reportError(value.getTokenSlice().getFirstToken(), CompileErrorCode.E7000);
            return NULL;
        }
        final var builder = irContext.getCurrentOrCreate();
        // TODO: print error message
        builder.trap();
        builder.unreachable(); // Make sure we terminate the current scope
        return NULL;
    }
}

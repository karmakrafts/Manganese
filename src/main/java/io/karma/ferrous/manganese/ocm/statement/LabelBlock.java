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

import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class LabelBlock implements LabeledStatement, Scope {
    private final Identifier scopeName;
    private final String name;
    private final TokenSlice tokenSlice;
    private final ArrayList<Statement> statements = new ArrayList<>();
    private Scope enclosingScope;

    public LabelBlock(final String name, final TokenSlice tokenSlice) {
        scopeName = new Identifier(name);
        this.name = name;
        this.tokenSlice = tokenSlice;
    }

    @Override
    public ScopeType getScopeType() {
        return ScopeType.LABEL_BLOCK;
    }

    @Override
    public Identifier getName() {
        return scopeName;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        irContext.popCurrent();
        return LLVMCore.LLVMBasicBlockAsValue(irContext.getAndPush(name).getBlockAddress());
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
    public String getLabelName() {
        return name;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }
}

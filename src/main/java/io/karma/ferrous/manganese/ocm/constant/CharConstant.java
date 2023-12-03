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

package io.karma.ferrous.manganese.ocm.constant;

import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.CharType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;

/**
 * @author Alexander Hinze
 * @since 09/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class CharConstant implements Constant {
    private final char value;
    private final TokenSlice tokenSlice;

    public CharConstant(final char value, final TokenSlice tokenSlice) {
        this.value = value;
        this.tokenSlice = tokenSlice;
    }

    @Override
    public Type getType() {
        return CharType.INSTANCE;
    }

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        return LLVMCore.LLVMConstInt(getType().materialize(targetMachine), value, false);
    }
}

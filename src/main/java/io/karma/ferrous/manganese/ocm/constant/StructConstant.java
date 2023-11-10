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

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.StructureType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;

import static org.lwjgl.llvm.LLVMCore.LLVMConstNamedStruct;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class StructConstant implements Constant {
    private final StructureType type;
    private final TokenSlice tokenSlice;
    private final Expression[] values;
    private Scope enclosingScope;

    public StructConstant(final StructureType type, final TokenSlice tokenSlice, final Expression... values) {
        this.type = type;
        this.tokenSlice = tokenSlice;
        this.values = values;
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

    // Constant

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        // TODO: pass in context from module somehow, maybe pass module in function?
        try (final var stack = MemoryStack.stackPush()) {
            final var values = Arrays.stream(this.values).mapToLong(expr -> expr.emit(targetMachine,
                irContext)).toArray();
            return LLVMConstNamedStruct(type.materialize(targetMachine), stack.pointers(values));
        }
    }
}

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

import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.Allocation;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.StorageMod;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

import static org.lwjgl.llvm.LLVMCore.LLVMSetValueName2;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class LetStatement implements Statement, NameProvider {
    private final Identifier name;
    private final Type type;
    private final boolean isMutable;
    private final EnumSet<StorageMod> storageMods;
    private final TokenSlice tokenSlice;
    private Expression value;
    private Scope enclosingScope;
    private Allocation allocation;

    public LetStatement(final Identifier name, final Type type, final @Nullable Expression value,
                        final boolean isMutable, final EnumSet<StorageMod> storageMods, final TokenSlice tokenSlice) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isMutable = isMutable;
        this.storageMods = storageMods;
        this.tokenSlice = tokenSlice;
    }

    public LetStatement(final Identifier name, final Expression value, final boolean isMutable,
                        final EnumSet<StorageMod> storageMods, final TokenSlice tokenSlice) {
        this(name, value.getType(), value, isMutable, storageMods, tokenSlice);
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(final @Nullable Expression value) {
        this.value = value;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public EnumSet<StorageMod> getStorageMods() {
        return storageMods;
    }

    public @Nullable Allocation getAllocation() {
        return allocation;
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
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

    // Statement

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext blockContext) {
        final var builder = blockContext.getCurrentOrCreate();
        if (!isMutable) {
            // For immutable variables, we can optimize by inlining the result in a register
            final var address = value.emit(targetMachine, blockContext);
            LLVMSetValueName2(address, name.toInternalName());
            allocation = Allocation.inRegister(address);
            return NULL;
        }
        // For mutable variables, we allocate some stack memory
        allocation = builder.alloca(type.materialize(targetMachine));
        LLVMSetValueName2(allocation.address(), name.toInternalName());
        if (value != null) { // Emit/store expression value
            builder.store(value.emit(targetMachine, blockContext), allocation.address());
        }
        return NULL;
    }
}

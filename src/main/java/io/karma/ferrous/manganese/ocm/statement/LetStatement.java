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
    private long immutableAddress;
    private long mutableAddress;
    private boolean hasChanged;

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

    public void setHasChanged(boolean hasChanged) {
        this.hasChanged = hasChanged;
    }

    public boolean hasChanged() {
        return hasChanged;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public EnumSet<StorageMod> getStorageMods() {
        return storageMods;
    }

    public long getMutableAddress() {
        return mutableAddress;
    }

    public long getImmutableAddress() {
        return immutableAddress;
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
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        immutableAddress = value.emit(targetMachine, irContext);
        if (!isMutable) {
            // For immutable variables, we can optimize by inlining the result in a register
            LLVMSetValueName2(immutableAddress, name.toInternalName());
            return NULL;
        }
        // For mutable variables, we allocate some stack memory
        mutableAddress = builder.alloca(type.materialize(targetMachine));
        final var internalName = name.toInternalName();
        LLVMSetValueName2(immutableAddress, String.format("val.%s", internalName));
        LLVMSetValueName2(mutableAddress, String.format("adr.%s", internalName));
        if (value != null) { // Emit/store expression value
            builder.store(value.emit(targetMachine, irContext), mutableAddress);
        }
        return NULL;
    }
}

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
import io.karma.ferrous.manganese.ocm.ValueStorage;
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
public final class LetStatement implements Statement, NameProvider, ValueStorage {
    private final Identifier name;
    private final Type type;
    private final boolean isMutable;
    private final boolean isInitialized;
    private final EnumSet<StorageMod> storageMods;
    private final TokenSlice tokenSlice;
    private Expression value;
    private Scope enclosingScope;
    private long immutableAddress;
    private long mutableAddress;
    private boolean hasChanged;

    public LetStatement(final Identifier name, final Type type, final Expression value, final boolean isMutable,
                        final boolean isInitialized, final EnumSet<StorageMod> storageMods,
                        final TokenSlice tokenSlice) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.isMutable = isMutable;
        this.isInitialized = isInitialized;
        this.storageMods = storageMods;
        this.tokenSlice = tokenSlice;
    }

    public LetStatement(final Identifier name, final Expression value, final boolean isMutable,
                        final boolean isInitialized, final EnumSet<StorageMod> storageMods,
                        final TokenSlice tokenSlice) {
        this(name, value.getType(), value, isMutable, isInitialized, storageMods, tokenSlice);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public EnumSet<StorageMod> getStorageMods() {
        return storageMods;
    }

    // ValueStorage

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void notifyChange() {
        hasChanged = true;
    }

    @Override
    public long loadFrom(final TargetMachine targetMachine, final IRContext irContext) {
        if (isMutable) { // If we are a mutable variable, load from stack memory
            if (!hasChanged) {
                return immutableAddress; // Optimize until we have been modified to avoid loads
            }
            return irContext.getCurrentOrCreate().load(type.materialize(targetMachine), mutableAddress);
        }
        return immutableAddress;
    }

    @Override
    public long storeInto(final long value, final TargetMachine targetMachine, final IRContext irContext) {
        if (!isMutable) {
            throw new IllegalStateException("Cannot store into immutable variable");
        }
        notifyChange();
        return irContext.getCurrentOrCreate().store(value, mutableAddress);
    }

    @Override
    public Expression getValue() {
        return value;
    }

    @Override
    public void setValue(final Expression value) {
        this.value = value;
        notifyChange();
    }

    @Override
    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public boolean hasChanged() {
        return hasChanged;
    }

    @Override
    public long getMutableAddress() {
        return mutableAddress;
    }

    @Override
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
        final var internalName = name.toInternalName();
        if (!isMutable) {
            // For immutable variables, we can optimize by inlining the result in a register
            immutableAddress = value.emit(targetMachine, irContext);
            LLVMSetValueName2(immutableAddress, internalName);
            return NULL;
        }
        // For mutable variables, we allocate some stack memory
        if (value != null) {
            immutableAddress = value.emit(targetMachine, irContext);
            LLVMSetValueName2(immutableAddress, String.format("imm.%s", internalName));
        }
        mutableAddress = builder.alloca(type.materialize(targetMachine));
        LLVMSetValueName2(mutableAddress, String.format("adr.%s", internalName));
        if (value != null) {
            builder.store(immutableAddress, mutableAddress);
        }
        return NULL;
    }
}

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
import io.karma.ferrous.manganese.ocm.expr.ReferenceExpression;
import io.karma.ferrous.manganese.ocm.field.FieldStorageProvider;
import io.karma.ferrous.manganese.ocm.field.FieldValueStorage;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.UDT;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.StorageMod;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.lwjgl.llvm.LLVMCore.LLVMSetValueName2;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class LetStatement implements Statement, NameProvider, ValueStorage, FieldStorageProvider {
    private final Identifier name;
    private final Type type;
    private final boolean isMutable;
    private final EnumSet<StorageMod> storageMods;
    private final TokenSlice tokenSlice;
    private final List<FieldValueStorage> fieldValues;
    private boolean isInitialized;
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
        if (type instanceof UDT udt) {
            fieldValues = udt.fields().stream().map(f -> new FieldValueStorage(f, this)).toList();
        }
        else {
            fieldValues = Collections.emptyList();
        }
    }

    public LetStatement(final Identifier name, final Expression value, final boolean isMutable,
                        final boolean isInitialized, final EnumSet<StorageMod> storageMods,
                        final TokenSlice tokenSlice) {
        this(name, value.getType(), value, isMutable, isInitialized, storageMods, tokenSlice);
    }

    public EnumSet<StorageMod> getStorageMods() {
        return storageMods;
    }

    public long getImmutableAddress() {
        return immutableAddress;
    }

    public long getMutableAddress() {
        return mutableAddress;
    }

    // ValueCarrier

    @Override
    public void setInitialized() {
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    // FieldStorageProvider

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public @Nullable FieldStorageProvider getParent() {
        return null;
    }

    @Override
    public List<FieldValueStorage> getFieldValues() {
        return fieldValues;
    }

    // ValueStorage

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        if (!isMutable) { // Allocate dynamically on the stack if needed
            final var address = builder.alloca(type.materialize(targetMachine));
            builder.store(immutableAddress, address);
            return address;
        }
        return mutableAddress;
    }

    @Override
    public long storeAddress(final @Nullable Expression exprValue, final long address,
                             final TargetMachine targetMachine, final IRContext irContext) {
        this.value = exprValue;
        return FieldStorageProvider.super.storeAddress(exprValue, address, targetMachine, irContext);
    }

    @Override
    public long store(final @Nullable Expression exprValue, final long value, final TargetMachine targetMachine,
                      final IRContext irContext) {
        this.value = exprValue;
        return FieldStorageProvider.super.store(exprValue, value, targetMachine, irContext);
    }

    @Override
    public long load(final TargetMachine targetMachine, final IRContext irContext) {
        if (!isMutable) { // If we are immutable, we can inline from register
            return immutableAddress;
        }
        return FieldStorageProvider.super.load(targetMachine, irContext);
    }

    @Override
    public void notifyChanged() {
        hasChanged = true;
    }

    @Override
    public Expression getValue() {
        return value;
    }

    @Override
    public boolean isMutable() {
        return isMutable;
    }

    @Override
    public boolean hasChanged() {
        return hasChanged;
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
            // For immutable variables, we can optimize by inlining the result into a register
            immutableAddress = value.emit(targetMachine, irContext);
            return NULL;
        }
        if (mutableAddress == NULL) {
            mutableAddress = builder.alloca(type.materialize(targetMachine));
        }
        LLVMSetValueName2(mutableAddress, internalName);
        if (value != null) {
            if (type.isReference() && value instanceof ReferenceExpression ref && ref.getReference() instanceof ValueStorage refStorage) {
                storeAddress(value, refStorage.getAddress(targetMachine, irContext), targetMachine, irContext);
            }
            else {
                store(value, targetMachine, irContext);
            }
        }
        return NULL;
    }
}

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

package io.karma.ferrous.manganese.ocm.field;

import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.UDT;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FieldValueStorage implements ValueStorage, FieldStorageProvider {
    private final Field field;
    private final FieldStorageProvider parent;
    private final List<FieldValueStorage> fieldValues;
    private Expression value;
    private boolean hasChanged;
    private boolean isInitialized;

    public FieldValueStorage(final Field field, final FieldStorageProvider parent) {
        this.field = field;
        this.parent = parent;
        isInitialized = parent.isInitialized();
        if (isInitialized) {
            value = field.getType().makeDefaultValue();
        }
        if (field.getType() instanceof UDT udt) {
            fieldValues = udt.fields().stream().map(f -> new FieldValueStorage(f, this)).toList();
        }
        else {
            fieldValues = Collections.emptyList();
        }
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

    @Override
    public Identifier getName() {
        return field.getName();
    }

    @Override
    public FieldStorageProvider getParent() {
        return parent;
    }

    // FieldStorageProvider

    @Override
    public Type getType() {
        return field.getType();
    }

    @Override
    public List<FieldValueStorage> getFieldValues() {
        return fieldValues;
    }

    // ValueStorage

    @Override
    public void notifyChanged() {
        hasChanged = true;
    }

    @Override
    public @Nullable Expression getValue() {
        return value;
    }

    @Override
    public boolean isMutable() {
        return field.isMutable();
    }

    @Override
    public boolean hasChanged() {
        return hasChanged;
    }

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var typeAddress = Objects.requireNonNull(parent.getType()).materialize(targetMachine);
        return builder.gep(typeAddress, parent.getAddress(targetMachine, irContext), field.getIndex());
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
        if (!isRootMutable()) {
            final var parentValue = parent.load(targetMachine, irContext);
            return irContext.getCurrentOrCreate().extract(parentValue, field.getIndex());
        }
        return FieldStorageProvider.super.load(targetMachine, irContext);
    }
}

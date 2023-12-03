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
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
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
public final class FieldStorage implements ValueStorage {
    private final Field field;
    private final ValueStorage parent;
    private final List<FieldStorage> fieldValues;
    private boolean hasChanged;
    private boolean isInitialized;

    public FieldStorage(final Field field, final @Nullable ValueStorage parent) {
        this.field = field;
        this.parent = parent;
        isInitialized = parent != null && parent.isInitialized();
        if (field.getType() instanceof UserDefinedType udt) {
            fieldValues = udt.fields().stream().map(f -> new FieldStorage(f, this)).toList();
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
    public @Nullable ValueStorage getParent() {
        return parent;
    }

    @Override
    public Type getType() {
        return field.getType();
    }

    // ValueStorage

    @Override
    public void notifyMutation() {
        hasChanged = true;
    }

    @Override
    public @Nullable Expression getValue() {
        return null; // TODO: fixme
    }

    @Override
    public boolean isMutable() {
        return field.isMutable();
    }

    @Override
    public boolean isMutated() {
        return hasChanged;
    }

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var type = parent.getType();
        final var typeAddress = Objects.requireNonNull(type).materialize(targetMachine);
        return builder.gep(typeAddress, parent.getAddress(targetMachine, irContext), field.getIndex());
    }
}

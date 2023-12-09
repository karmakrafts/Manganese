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
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FieldStorage implements ValueStorage {
    private final Field field;
    private final ValueStorage parent;
    private final Map<Identifier, FieldStorage> fieldStorages;
    private Expression value;
    private boolean hasChanged;
    private boolean isInitialized;

    public FieldStorage(final Field field, final @Nullable ValueStorage parent) {
        this.field = field;
        this.parent = parent;
        isInitialized = parent != null && parent.isInitialized();
        if (field.getType() instanceof UserDefinedType udt) { // @formatter:off
            fieldStorages = udt.fields()
                .stream()
                .map(f -> Pair.of(f.getName(), new FieldStorage(f, this)))
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        } // @formatter:on
        else {
            fieldStorages = Collections.emptyMap();
        }
    }

    @Override
    public @Nullable ValueStorage getField(final Identifier name) {
        return fieldStorages.get(name);
    }

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

    @Override
    public void notifyMutation() {
        hasChanged = true;
    }

    @Override
    public @Nullable Expression getValue() {
        return value;
    }

    @Override
    public void setValue(final @Nullable Expression value) {
        this.value = value;
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

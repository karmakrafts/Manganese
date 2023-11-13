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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 09/11/2023
 */
@API(status = API.Status.INTERNAL)
public interface ValueStorage extends NameProvider {
    @Nullable Expression getValue();

    void setInitialized();

    boolean isInitialized();

    default @Nullable Type getType() {
        final var value = getValue();
        if (value == null) {
            return null;
        }
        return value.getType();
    }

    boolean isMutable();

    void notifyChanged();

    boolean hasChanged();

    long getAddress(final TargetMachine targetMachine, final IRContext irContext);

    @SuppressWarnings("unused")
    default long storeAddress(final @Nullable Expression exprValue, final long address,
                              final TargetMachine targetMachine, final IRContext irContext) {
        final var type = Objects.requireNonNull(getType());
        if (!type.isReference()) {
            throw new IllegalStateException("Cannot store address into non-reference value storage");
        }
        notifyChanged();
        return irContext.getCurrentOrCreate().store(address, getAddress(targetMachine, irContext));
    }

    default long store(final @Nullable Expression exprValue, final long value, final TargetMachine targetMachine,
                       final IRContext irContext) {
        if (!isMutable()) {
            throw new IllegalStateException("Cannot store into immutable value storage");
        }
        notifyChanged();
        final var builder = irContext.getCurrentOrCreate();
        final var type = Objects.requireNonNull(getType());
        var address = getAddress(targetMachine, irContext);
        if (type.isReference()) { // Load in ref pointer before storing
            address = builder.load(type.materialize(targetMachine), address);
        }
        return builder.store(value, address);
    }

    default long store(final Expression value, final TargetMachine targetMachine, final IRContext irContext) {
        return store(value, value.emit(targetMachine, irContext), targetMachine, irContext);
    }

    default long load(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var value = getAddress(targetMachine, irContext);
        final var type = Objects.requireNonNull(getType());
        if (type.isReference()) { // Strip ref pointer
            return builder.load(type.getBaseType().materialize(targetMachine),
                builder.load(type.materialize(targetMachine), value));
        }
        return builder.load(type.materialize(targetMachine), value);
    }
}

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
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 09/11/2023
 */
@API(status = API.Status.INTERNAL)
public interface ValueStorage extends Named {
    @Nullable Expression getValue();

    void setValue(final @Nullable Expression value);

    default @Nullable ValueStorage getParent() {
        return null;
    }

    @Nullable ValueStorage getField(final Identifier name);

    void setInitialized();

    boolean isInitialized();

    boolean isMutable();

    boolean isMutated();

    void notifyMutation();

    Type getType();

    default boolean isRootMutable() {
        var current = getParent();
        while (current != null) {
            final var next = current.getParent();
            if (next == null) {
                break;
            }
            current = next;
        }
        if (current == null) {
            return isMutable();
        }
        return current.isRootMutable();
    }

    long getAddress(final TargetMachine targetMachine, final IRContext irContext);

    default long load(final TargetMachine targetMachine, final IRContext irContext) {
        final var address = getAddress(targetMachine, irContext);
        final var typeAddress = getType().materialize(targetMachine);
        return irContext.getCurrentOrCreate().load(typeAddress, address);
    }

    default long store(final long value, final TargetMachine targetMachine, final IRContext irContext) {
        final var address = getAddress(targetMachine, irContext);
        notifyMutation();
        return irContext.getCurrentOrCreate().store(value, address);
    }
}

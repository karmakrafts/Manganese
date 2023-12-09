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

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToLongFunction;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 09/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class ParameterStorage implements ValueStorage {
    private final Parameter parameter;
    private final ToLongFunction<CompileContext> addressProvider;
    private Expression value;
    private long mutableAddress;
    private boolean isInitialized;
    private boolean isMutated;

    public ParameterStorage(final Parameter parameter, final ToLongFunction<CompileContext> addressProvider) {
        this.parameter = parameter;
        this.addressProvider = addressProvider;
        final var defaultValue = parameter.getDefaultValue();
        value = defaultValue;
        isInitialized = defaultValue != null;
    }

    @Override
    public @Nullable ValueStorage getField(final Identifier name) {
        return null;
    }

    @Override
    public Identifier getName() {
        return parameter.getName();
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
    public void setInitialized() {
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    @Override
    public boolean isMutable() {
        return parameter.isMutable();
    }

    @Override
    public boolean isMutated() {
        return isMutated;
    }

    @Override
    public void notifyMutation() {
        isMutated = true;
    }

    @Override
    public Type getType() {
        return parameter.getType();
    }

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        if (!parameter.isMutable()) {
            throw new IllegalStateException("Cannot obtain address of immutable parameter");
        }
        if (mutableAddress == NULL) {
            final var builder = irContext.getCurrentOrCreate();
            mutableAddress = builder.alloca(getType().materialize(targetMachine));
            builder.store(addressProvider.applyAsLong(irContext.getCompileContext()), mutableAddress);
        }
        return mutableAddress;
    }

    @Override
    public long load(final TargetMachine targetMachine, final IRContext irContext) {
        if (isMutated) {
            // If the parameter was mutated, load from address
            return ValueStorage.super.load(targetMachine, irContext);
        }
        return addressProvider.applyAsLong(irContext.getCompileContext());
    }
}

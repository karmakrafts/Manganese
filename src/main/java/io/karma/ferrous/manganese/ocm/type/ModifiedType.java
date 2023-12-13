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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 12/12/2023
 */
@API(status = API.Status.INTERNAL)
public final class ModifiedType implements Type {
    private final Type baseType;
    private final EnumSet<TypeModifier> modifiers;

    public ModifiedType(final Type baseType, final EnumSet<TypeModifier> modifiers) {
        this.baseType = baseType;
        this.modifiers = modifiers;
    }

    @Override
    public boolean isDerived() {
        return true; // Modified types are derived
    }

    @Override
    public String getMangledName() {
        final var builder = new StringBuilder(baseType.getMangledName());
        for (final var mod : modifiers) {
            builder.append(mod.getMangledSymbol());
        }
        return builder.toString();
    }

    @Override
    public List<TypeModifier> getModifiers() {
        return new ArrayList<>(modifiers);
    }

    @Override
    public Identifier getName() {
        return baseType.getName();
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return baseType.getEnclosingScope();
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return baseType.materialize(machine);
    }

    @Override
    public Expression makeDefaultValue(TargetMachine targetMachine) {
        return baseType.makeDefaultValue(targetMachine);
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(baseType, modifiers);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ModifiedType type) { // @formatter:off
            return baseType.equals(type.baseType)
                && modifiers.equals(type.modifiers);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return STR."\{baseType} \{modifiers}";
    }
}

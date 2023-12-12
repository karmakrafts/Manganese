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

import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public interface Type extends Scoped, Mangleable {
    long materialize(final TargetMachine machine);

    Expression makeDefaultValue(final TargetMachine targetMachine);

    Type getBaseType();

    default long cast(final Type type, final long value, final TargetMachine targetMachine, final IRContext irContext) {
        return value;
    }

    default TypeKind getKind() {
        return TypeKind.UDT;
    }

    default TokenSlice getTokenSlice() {
        return TokenSlice.EMPTY;
    }

    default List<TypeAttribute> getAttributes() {
        return Collections.emptyList();
    }

    default List<GenericParameter> getGenericParams() {
        return Collections.emptyList();
    }

    default List<TypeModifier> getModifiers() {
        return Collections.emptyList();
    }

    default Type monomorphize(final List<Type> genericTypes) {
        return this;
    }

    default @Nullable GenericParameter getGenericParam(final String name) {
        final var params = getGenericParams();
        for (final GenericParameter param : params) {
            if (!param.getName().toString().equals(name)) {
                continue;
            }
            return param;
        }
        return null;
    }

    default int getSize(final TargetMachine targetMachine) {
        return targetMachine.getTypeSize(materialize(targetMachine));
    }

    default int getAlignment(final TargetMachine targetMachine) {
        return targetMachine.getTypeAlignment(materialize(targetMachine));
    }

    default boolean canAccept(final TargetMachine targetMachine, final Type type) {
        if (type.isRef()) {
            return type.getBaseType() == this;
        }
        return type == this;
    }

    default boolean canBeCastFrom(final TargetMachine targetMachine, final Type type) {
        return canAccept(targetMachine, type);
    }

    default boolean isImaginary() {
        return false;
    }

    default boolean isAliased() {
        return false;
    }

    default boolean isBuiltin() {
        return getKind().isBuiltin();
    }

    default boolean isComplete() {
        return true;
    }

    default boolean isMonomorphic() {
        return getBaseType().isMonomorphic();
    }

    default boolean isDerived() {
        return false;
    }

    default Type derive(final @Nullable TypeAttribute attribute, final TypeModifier... modifiers) {
        // @formatter:off
        return Types.cached(new DerivedType(this, attribute, modifiers.length > 0
            ? EnumSet.copyOf(Arrays.asList(modifiers))
            : EnumSet.noneOf(TypeModifier.class)));
        // @formatter:on
    }

    default Type derive(final TypeModifier... modifiers) {
        return derive(null, modifiers);
    }

    default Type derive(final Collection<TypeAttribute> attributes) {
        var type = this;
        for (final var attrib : attributes) {
            type = type.derive(attrib);
        }
        return type;
    }

    default Type asPtr(final TypeModifier... modifiers) {
        return derive(TypeAttribute.POINTER, modifiers);
    }

    default Type asRef(final TypeModifier... modifiers) {
        return derive(TypeAttribute.REFERENCE, modifiers);
    }

    default boolean isRef() {
        final var attributes = getAttributes();
        if (attributes.isEmpty()) {
            return false;
        }
        return attributes.getLast() == TypeAttribute.REFERENCE;
    }

    default boolean isPtr() {
        final var attributes = getAttributes();
        if (attributes.isEmpty()) {
            return false;
        }
        return attributes.getLast() == TypeAttribute.POINTER;
    }
}

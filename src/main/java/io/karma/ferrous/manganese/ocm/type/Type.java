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

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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

    default EnumSet<TypeModifier> getModifiers() {
        return getBaseType().getModifiers();
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

    /**
     * @param type The type to check against.
     * @return True if the given type may be assigned or
     * implicitly casted to this type. False otherwise.
     */
    default boolean canAccept(final Type type) {
        if (type.isReference()) {
            return type.getBaseType() == this;
        }
        return type == this;
    }

    /**
     * @return True if this type is imaginary and cannot be materialized
     * into a runtime structure.
     */
    default boolean isImaginary() {
        return false;
    }

    /**
     * @return True if this type is aliased and refers to another
     * type or alias.
     */
    default boolean isAliased() {
        return false;
    }

    /**
     * @return True if this is a builtin type.
     */
    default boolean isBuiltin() {
        return false;
    }

    /**
     * @return True if this is a complete type.
     * False if this type is incomplete and missing and associated data layout.
     */
    default boolean isComplete() {
        return true;
    }

    /**
     * @return True if this is a monomorphic type
     * (when it has no generic parameters associated with it).
     */
    default boolean isMonomorphic() {
        return getBaseType().isMonomorphic();
    }

    default boolean isDerived() {
        return false;
    }

    default Type derive(final TypeAttribute attribute) {
        return Types.cached(new DerivedType(this, attribute));
    }

    default Type derive(final Collection<TypeAttribute> attributes) {
        var type = this;
        for (final var attrib : attributes) {
            type = type.derive(attrib);
        }
        return type;
    }

    default Type asPtr() {
        return derive(TypeAttribute.POINTER);
    }

    default Type asRef() {
        return derive(TypeAttribute.REFERENCE);
    }

    default boolean isReference() {
        final var attributes = getAttributes();
        if (attributes.isEmpty()) {
            return false;
        }
        return attributes.getLast() == TypeAttribute.REFERENCE;
    }

    default boolean isPointer() {
        final var attributes = getAttributes();
        if (attributes.isEmpty()) {
            return false;
        }
        return attributes.getLast() == TypeAttribute.POINTER;
    }
}

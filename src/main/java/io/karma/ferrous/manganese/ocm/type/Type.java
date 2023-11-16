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
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TypeMod;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public interface Type extends Scoped {
    long materialize(final TargetMachine machine);

    TypeAttribute[] getAttributes();

    Type getBaseType();

    Expression makeDefaultValue();

    TokenSlice getTokenSlice();

    GenericParameter[] getGenericParams();

    default EnumSet<TypeMod> getModifiers() {
        return getBaseType().getModifiers();
    }

    default @Nullable GenericParameter getGenericParam(final String name) {
        final var params = getGenericParams();
        for (final GenericParameter param : params) {
            if (!param.getName().equals(name)) {
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
        return getBaseType().isBuiltin();
    }

    /**
     * @return True if this is a complete type.
     * False if this type is incomplete and missing and associated data layout.
     */
    default boolean isComplete() {
        return getBaseType().isComplete();
    }

    default Type deriveWithMods(final TypeMod... modifiers) {
        return Types.cached(new DerivedType(this, null, KitchenSink.of(TypeMod[]::new, modifiers)));
    }

    default Type deriveGeneric(final Expression... values) {
        final var type = new DerivedType(this, null, EnumSet.noneOf(TypeMod.class));
        final var params = type.getGenericParams();
        final var numParams = params.length;
        if (values.length > numParams) {
            throw new IllegalArgumentException("Invalid number of values");
        }
        for (var i = 0; i < numParams; i++) {
            params[i].setValue(values[i]);
        }
        return Types.cached(type);
    }

    default Type derive(final TypeAttribute attribute) {
        return Types.cached(new DerivedType(this, attribute, EnumSet.noneOf(TypeMod.class)));
    }

    default Type derive(final TypeAttribute[] attributes) {
        var result = this;
        for (final var attrib : attributes) {
            result = switch (attrib) {
                case POINTER -> result.derivePointer();
                case REFERENCE -> result.deriveReference();
            };
        }
        return result;
    }

    default Type derivePointer() {
        return derive(TypeAttribute.POINTER);
    }

    default Type derivePointer(final int depth) {
        var result = this;
        for (var i = 0; i < depth; i++) {
            result = result.derivePointer();
        }
        return result;
    }

    default Type deriveReference() {
        return derive(TypeAttribute.REFERENCE);
    }

    default boolean isReference() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.REFERENCE;
    }

    default boolean isPointer() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.POINTER;
    }
}

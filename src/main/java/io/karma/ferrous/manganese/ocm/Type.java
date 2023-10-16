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

import io.karma.ferrous.manganese.scope.ScopeProvider;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public interface Type extends ScopeProvider {
    long materialize(final Target target);

    TypeAttribute[] getAttributes();

    Type getBaseType();

    default @Nullable Type getEnclosingType() {
        final var scope = getEnclosingScope();
        if (scope instanceof Type type) {
            return type;
        }
        return null;
    }

    default void setEnclosingType(final Type enclosingType) {
        setEnclosingScope(enclosingType);
    }

    @Override
    default Identifier getName() {
        return getBaseType().getName();
    }

    default boolean isBuiltin() {
        return getBaseType().isBuiltin();
    }

    default boolean isComplete() {
        return getBaseType().isComplete();
    }

    default Type derive(final TypeAttribute... attributes) {
        return Types.cached(new DerivedType(this, attributes));
    }

    default Type derivePointer(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.POINTER);
        return derive(attribs);
    }

    default Type deriveSlice(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.SLICE);
        return derive(attribs);
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

    default boolean isSlice() {
        final var attributes = getAttributes();
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.SLICE;
    }
}

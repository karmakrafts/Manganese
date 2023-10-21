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

import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class Types {
    private static final HashMap<String, Type> CACHE = new HashMap<>();

    // @formatter:off
    private Types() {}
    // @formatter:on

    public static void invalidateCache() {
        CACHE.clear();
    }

    @SuppressWarnings("unchecked")
    static <T extends Type> T cached(final T type) {
        final var key = type.toString();
        final var result = CACHE.get(key);
        if (result != null) {
            return (T) result;
        }
        CACHE.put(key, type);
        return type;
    }

    public static Optional<BuiltinType> builtin(final Identifier name) { // @formatter:off
        return Arrays.stream(BuiltinType.values())
            .filter(type -> type.getName().equals(name))
            .findFirst();
    } // @formatter:on

    public static FunctionType function(final Type returnType, final List<? extends Type> paramTypes,
                                        final boolean isVarArg) {
        return cached(new FunctionType(returnType, isVarArg, paramTypes.toArray(Type[]::new)));
    }

    public static FunctionType function(final Type returnType, final boolean isVarArg, final Type... paramTypes) {
        return cached(new FunctionType(returnType, isVarArg, paramTypes));
    }

    public static NamedFunctionType namedFunction(final Identifier name, final Type returnType,
                                                  final List<? extends Type> paramTypes, final boolean isVarArg) {
        return cached(new NamedFunctionType(name, returnType, isVarArg, paramTypes.toArray(Type[]::new)));
    }

    public static NamedFunctionType namedFunction(final Identifier name, final Type returnType, final boolean isVarArg,
                                                  final Type... paramTypes) {
        return cached(new NamedFunctionType(name, returnType, isVarArg, paramTypes));
    }

    public static StructureType structure(final Identifier name, final boolean isPacked, final Type... fieldTypes) {
        return cached(new StructureType(name, isPacked, fieldTypes));
    }

    public static StructureType structure(final Identifier name, final Type... fieldTypes) {
        return structure(name, false, fieldTypes);
    }

    public static IncompleteType incomplete(final Identifier name) {
        return cached(new IncompleteType(name));
    }

    public static AliasedType aliased(final Identifier name, final Type backingType) {
        return cached(new AliasedType(name, backingType));
    }

    public static TupleType tuple(final Type... types) {
        return cached(new TupleType(types));
    }

    public static VectorType vector(final Type type, final int elementCount) {
        return cached(new VectorType(type, elementCount));
    }
}

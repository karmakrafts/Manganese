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

import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.NamedFunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.Identifier;

import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public record Function(Identifier identifier, boolean isExtern, boolean isVarArg, Type returnType,
                       Parameter... params) {
    public Function(final Identifier identifier, final Type returnType, final Parameter... params) {
        this(identifier, false, false, returnType, params);
    }

    public FunctionType makeType() {
        final var paramTypes = Arrays.stream(params).map(Parameter::type).toList();
        return Types.function(returnType, paramTypes, isVarArg);
    }

    public NamedFunctionType makeNamedType(final Identifier name) {
        final var paramTypes = Arrays.stream(params).map(Parameter::type).toList();
        return Types.namedFunction(name, returnType, paramTypes, isVarArg);
    }
}

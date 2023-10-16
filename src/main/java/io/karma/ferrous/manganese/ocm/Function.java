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

import io.karma.ferrous.manganese.ocm.scope.EnclosingScopeProvider;
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
public final class Function implements EnclosingScopeProvider {
    private final Identifier name;
    private final boolean isExtern;
    private final boolean isVarArg;
    private final Type returnType;
    private final Parameter[] parameters;
    private EnclosingScopeProvider enclosingScope;

    public Function(Identifier name, boolean isExtern, boolean isVarArg, Type returnType, Parameter... params) {
        this.name = name;
        this.isExtern = isExtern;
        this.isVarArg = isVarArg;
        this.returnType = returnType;
        this.parameters = params;
    }

    public Function(final Identifier name, final Type returnType, final Parameter... params) {
        this(name, false, false, returnType, params);
    }

    public boolean isExtern() {
        return isExtern;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final EnclosingScopeProvider scope) {
        enclosingScope = scope;
    }

    public FunctionType makeType() {
        final var paramTypes = Arrays.stream(parameters).map(Parameter::type).toList();
        return Types.function(returnType, paramTypes, isVarArg);
    }

    public NamedFunctionType makeNamedType(final Identifier name) {
        final var paramTypes = Arrays.stream(parameters).map(Parameter::type).toList();
        return Types.namedFunction(name, returnType, paramTypes, isVarArg);
    }
}

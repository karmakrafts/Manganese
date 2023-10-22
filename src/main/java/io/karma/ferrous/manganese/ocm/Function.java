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
import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.scope.Scoped;
import io.karma.ferrous.manganese.util.CallingConvention;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class Function implements NameProvider, Scoped {
    private final Identifier name;
    private final CallingConvention callConv;
    private final boolean isExtern;
    private final boolean isVarArg;
    private final Type returnType;
    private final Parameter[] parameters;
    private Scope enclosingScope;

    public Function(final Identifier name, final CallingConvention callConv, final boolean isExtern,
                    final boolean isVarArg, final Type returnType, final Parameter... params) {
        this.name = name;
        this.callConv = callConv;
        this.isExtern = isExtern;
        this.isVarArg = isVarArg;
        this.returnType = returnType;
        this.parameters = params;
    }

    public Function(final Identifier name, final CallingConvention callConv, final Type returnType,
                    final Parameter... params) {
        this(name, callConv, false, false, returnType, params);
    }

    public Function(final Identifier name, final Type returnType, final Parameter... params) {
        this(name, CallingConvention.CDECL, false, false, returnType, params);
    }

    public CallingConvention getCallConv() {
        return callConv;
    }

    public boolean isExtern() {
        return isExtern;
    }

    public FunctionType makeType() {
        final var paramTypes = Arrays.stream(parameters).map(Parameter::type).toList();
        return Types.function(returnType, paramTypes, isVarArg);
    }

    public NamedFunctionType makeNamedType(final Identifier name) {
        final var paramTypes = Arrays.stream(parameters).map(Parameter::type).toList();
        return Types.namedFunction(name, returnType, paramTypes, isVarArg);
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Object

    @Override
    public String toString() {
        return String.format("%s %s(%s)", returnType, name, Arrays.toString(parameters));
    }
}

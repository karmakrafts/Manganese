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

package io.karma.ferrous.manganese.parser;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ProtoFunctionParser extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private final boolean isExtern;
    private final TokenSlice tokenSlice;
    private Function function;

    public ProtoFunctionParser(final Compiler compiler, final CompileContext compileContext,
                               final ScopeStack capturedScopeStack, final boolean isExtern,
                               final TokenSlice tokenSlice) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
        this.isExtern = isExtern;
        this.tokenSlice = tokenSlice;
    }

    @Override
    public void enterProtoFunction(final ProtoFunctionContext context) {
        if (function != null) {
            return;
        }
        final var name = FunctionUtils.parseFunctionName(context.functionIdent());
        final var callConv = FunctionUtils.parseCallingConvention(compileContext, context);
        final var type = FunctionUtils.parseFunctionType(compiler, compileContext, capturedScopeStack, context);
        final var paramNames = FunctionUtils.parseParameterNames(context);
        final var paramTypes = type.getParamTypes();
        final var numParams = paramTypes.length;
        var expectedNumParams = paramNames.length;
        if (type.isVarArg()) {
            expectedNumParams--; // Account for trailing ...
        }
        if (numParams != expectedNumParams) {
            throw new IllegalStateException("Invalid function parser state");
        }
        final var params = new Parameter[numParams];
        for (var i = 0; i < numParams; i++) {
            params[i] = new Parameter(paramNames[i], paramTypes[i], null);
        }
        function = capturedScopeStack.applyEnclosingScopes(new Function(name,
            callConv,
            isExtern,
            type.isVarArg(),
            type.getReturnType(),
            tokenSlice,
            params));
        super.enterProtoFunction(context);
    }

    public Function getFunction() {
        return function;
    }
}

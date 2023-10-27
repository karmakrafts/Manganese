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

package io.karma.ferrous.manganese.translate;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.util.CallingConvention;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class FunctionParser extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private Identifier identifier;
    private FunctionType type;
    private CallingConvention callConv;
    private Identifier[] paramNames = new Identifier[0];

    public FunctionParser(final Compiler compiler, final CompileContext compileContext, final ScopeStack capturedScopeStack) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        identifier = FunctionUtils.getFunctionName(context.functionIdent());
        callConv = FunctionUtils.getCallingConvention(compileContext, context);
        this.type = TypeUtils.getFunctionType(compiler, compileContext, capturedScopeStack, context);
        paramNames = FunctionUtils.getParameterNames(context);
        super.enterProtoFunction(context);
    }

    public Function getFunction() {
        final var paramTypes = type.getParamTypes();
        final var numParams = paramTypes.length;
        if (numParams != paramNames.length) {
            throw new IllegalStateException("Invalid function parser state");
        }
        final var params = new Parameter[numParams];
        for (var i = 0; i < numParams; i++) {
            params[i] = new Parameter(paramNames[i], paramTypes[i], null);
        }
        return capturedScopeStack.applyEnclosingScopes(
                new Function(identifier, callConv, type.getReturnType(), params));
    }
}

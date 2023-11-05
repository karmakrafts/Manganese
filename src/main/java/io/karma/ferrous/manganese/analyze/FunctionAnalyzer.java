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

package io.karma.ferrous.manganese.analyze;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.FunctionBodyContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class FunctionAnalyzer extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private final TokenSlice tokenSlice;
    private Function function;

    public FunctionAnalyzer(final Compiler compiler, final CompileContext compileContext,
                            final ScopeStack capturedScopeStack, final TokenSlice tokenSlice) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
        this.tokenSlice = tokenSlice;
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        if (function != null) {
            return;
        }
        final var identifier = FunctionUtils.getFunctionName(context.functionIdent());
        final var callConv = FunctionUtils.getCallingConvention(compileContext, context);
        final var type = FunctionUtils.getFunctionType(compiler, compileContext, capturedScopeStack, context);
        final var paramNames = FunctionUtils.getParameterNames(context);
        final var paramTypes = type.getParamTypes();
        final var numParams = paramTypes.length;
        if (numParams != paramNames.length) {
            throw new IllegalStateException("Invalid function parser state");
        }
        final var params = new Parameter[numParams];
        for (var i = 0; i < numParams; i++) {
            params[i] = new Parameter(paramNames[i], paramTypes[i], null);
        }
        function = capturedScopeStack.applyEnclosingScopes(new Function(identifier,
            callConv,
            type.getReturnType(),
            tokenSlice,
            params));
        super.enterProtoFunction(context);
    }

    @Override
    public void enterFunctionBody(final FunctionBodyContext context) {
        if (function.getBody() != null) {
            return;
        }
        final var analyzer = new FunctionBodyAnalyzer(compiler, compileContext);
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        function.createBody(analyzer.getStatements().toArray(Statement[]::new));
        super.enterFunctionBody(context);
    }

    public Function getFunction() {
        return function;
    }
}

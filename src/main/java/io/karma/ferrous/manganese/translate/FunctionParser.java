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
import io.karma.ferrous.manganese.compiler.CompileStatus;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.scope.ScopeStack;
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

    public FunctionParser(final Compiler compiler, final ScopeStack capturedScopeStack) {
        super(compiler);
        this.capturedScopeStack = capturedScopeStack;
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        identifier = FunctionUtils.getFunctionName(context.functionIdent());
        callConv = FunctionUtils.getCallingConvention(compiler, context);
        // @formatter:off
        final var type = TypeUtils.getFunctionType(compiler, capturedScopeStack, context)
            .unwrapOrReport(compiler, context.start, CompileStatus.TRANSLATION_ERROR);
        // @formatter:on
        if (type.isEmpty()) {
            return;
        }
        this.type = type.get();
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
        return new Function(identifier, callConv, type.getReturnType(), params);
    }
}

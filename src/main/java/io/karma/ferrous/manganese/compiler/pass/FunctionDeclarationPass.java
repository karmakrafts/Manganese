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

package io.karma.ferrous.manganese.compiler.pass;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.AttributeUsage;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.FunctionModifier;
import io.karma.ferrous.manganese.ocm.function.Parameter;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.parser.ParseAdapter;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionDeclarationPass implements CompilePass {
    @Override
    public void run(final CompileContext compileContext, final Module module, final ExecutorService executor) {
        final var compiler = compileContext.getCompiler();
        final var profiler = compiler.getProfiler();
        profiler.push();
        compileContext.walkParseTree(new ParseListenerImpl(compileContext));
        resolveFunctionTypes(compileContext);
        materializeFunctionTypes(compiler, compileContext, module);
        profiler.pop();
    }

    private void resolveFunctionTypes(final CompileContext compileContext) {
        final var profiler = compileContext.getCompiler().getProfiler();
        profiler.push();
        final var moduleData = compileContext.getOrCreateModuleData();
        final var overloadSets = moduleData.getFunctions().values();
        for (final var overloadSet : overloadSets) {
            for (final var function : overloadSet.values()) {
                final var params = function.getParameters();
                for (final var param : params) {
                    final var type = param.getType();
                    final var completeType = moduleData.findCompleteType(type);
                    if (completeType == null) {
                        compileContext.reportError(type.getTokenSlice().getFirstToken(), CompileErrorCode.E3005);
                        continue;
                    }
                    param.setType(completeType);
                }
            }
        }
        profiler.pop();
    }

    public void materializeFunctionTypes(final Compiler compiler, final CompileContext compileContext,
                                         final Module module) {
        Logger.INSTANCE.debugln("Pre-materializing function prototypes");
        final var profiler = compileContext.getCompiler().getProfiler();
        profiler.push();
        final var moduleData = compileContext.getOrCreateModuleData();
        final var overloadSets = moduleData.getFunctions().values();
        for (final var overloadSet : overloadSets) {
            final var functions = overloadSet.values();
            for (final var function : functions) {
                function.materialize(module, compiler.getTargetMachine());
            }
        }
        profiler.pop();
    }

    private static final class ParseListenerImpl extends ParseAdapter {
        public ParseListenerImpl(final CompileContext compileContext) {
            super(compileContext);
        }

        @Override
        public void enterFunction(final FerrousParser.FunctionContext context) {
            /* don't manipulate the scope stack */
        }

        @Override
        public void exitFunction(final FerrousParser.FunctionContext context) {
            /* don't manipulate the scope stack */
        }

        @Override
        public void enterProtoFunction(final ProtoFunctionContext context) {
            if (checkIsFunctionAlreadyDefined(context)) {
                return;
            }
            final var name = FunctionUtils.parseFunctionName(context.functionIdent());
            final var callConv = FunctionUtils.parseCallingConvention(compileContext, context);
            final var type = FunctionUtils.parseFunctionType(compileContext, scopeStack, context);
            final var params = Parameter.parse(compileContext, scopeStack, context.functionParamList());
            final var genericParams = GenericParameter.parse(compileContext, scopeStack, context.genericParamList());
            final var attributeUsages = AttributeUsage.parse(compileContext, scopeStack, context.attributeList());
            final var modifiers = FunctionModifier.parse(compileContext, context.functionMod());
            final var function = scopeStack.applyEnclosingScopes(new Function(name,
                callConv,
                type,
                modifiers,
                TokenSlice.from(compileContext, context),
                params,
                genericParams,
                attributeUsages));
            compileContext.getOrCreateModuleData().getFunctions().computeIfAbsent(function.getQualifiedName(),
                n -> new HashMap<>()).put(function.getType(), function);
            super.enterProtoFunction(context); // Make sure we pick up the default scope for function prototypes
        }

        private boolean checkIsFunctionAlreadyDefined(final ProtoFunctionContext context) {
            final var identContext = context.functionIdent();
            final var name = FunctionUtils.parseFunctionName(identContext);
            final var overloadSet = compileContext.getOrCreateModuleData().getFunctions().get(name);
            if (overloadSet != null) {
                final var type = FunctionUtils.parseFunctionType(compileContext, scopeStack, context);
                final var function = overloadSet.get(type);
                if (function != null) {
                    final var message = KitchenSink.makeCompilerMessage(String.format("Function '%s' is already defined",
                        name));
                    compileContext.reportError(identContext.start, message, CompileErrorCode.E4003);
                    return true;
                }
            }
            return false;
        }
    }
}

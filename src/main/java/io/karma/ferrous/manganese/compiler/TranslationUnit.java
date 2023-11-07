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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
@API(status = Status.INTERNAL)
public class TranslationUnit extends ParseAdapter {
    private final Module module;
    private boolean isDisposed;

    public TranslationUnit(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
        final var targetMachine = compiler.getTargetMachine();
        module = targetMachine.createModule(compileContext.getCurrentModuleName());
    }

    private @Nullable Function getFunction(final ProtoFunctionContext context) {
        final var name = FunctionUtils.getFunctionName(context.functionIdent());
        final var scopeName = scopeStack.getScopeName();
        final var type = FunctionUtils.getFunctionType(compiler, compileContext, scopeStack, context);
        final var function = compileContext.getAnalyzer().findFunctionInScope(name, scopeName, type);
        if (function == null) {
            compileContext.reportError(Utils.makeCompilerMessage(name.toString()), CompileErrorCode.E5000);
        }
        return function;
    }

    //@Override
    //public void enterExternFunction(final ExternFunctionContext context) {
    //    final var function = getFunction(context.protoFunction());
    //    if (function == null) {
    //        return; // TODO: log warning/error?
    //    }
    //    function.materialize(compileContext, module, compiler.getTargetMachine());
    //    super.enterExternFunction(context);
    //}

    //@Override
    //public void enterFunction(final FunctionContext context) {
    //    final var function = getFunction(context.protoFunction());
    //    if (function == null) {
    //        return; // TODO: log warning/error?
    //    }
    //    ParseTreeWalker.DEFAULT.walk(new FunctionAnalyzer(compiler, compileContext, function), context);
    //    function.materialize(compileContext, module, compiler.getTargetMachine());
    //    super.enterFunction(context);
    //}

    public Module getModule() {
        return module;
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        module.dispose();
        isDisposed = true;
    }
}

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
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.vanadium.FerrousParser.ExternFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.FunctionContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.HashMap;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
@API(status = Status.INTERNAL)
public class TranslationUnit extends ParseAdapter {
    private final HashMap<Identifier, Function> functions = new HashMap<>();
    private final Module module;
    private boolean isDisposed;

    public TranslationUnit(final Compiler compiler, final String name) {
        super(compiler);
        module = compiler.getTargetMachine().createModule(name);
    }

    @Override
    public void enterFunction(final FunctionContext context) {
        final var parser = new FunctionParser(compiler, scopeStack);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        final var function = parser.getFunction();
        functions.put(function.getQualifiedName(), function);
        Logger.INSTANCE.debugln("Parsed function '%s'", function);
        super.enterFunction(context);
    }

    @Override
    public void enterExternFunction(final ExternFunctionContext context) {
        final var prototype = context.protoFunction();
        final var type = TypeUtils.getFunctionType(compiler, scopeStack, prototype);
        final var name = FunctionUtils.getFunctionName(prototype.functionIdent()).toString();
        final var functionType = type.materialize(compiler.getTargetMachine());
        final var function = LLVMAddFunction(module.getAddress(), name, functionType);
        if (function == NULL) {
            final var compileContext = compiler.getContext();
            compileContext.reportError(compileContext.makeError(context.start, CompileErrorCode.E4000));
            return;
        }
        LLVMSetLinkage(function, LLVMExternalLinkage);
        LLVMSetFunctionCallConv(function, FunctionUtils.getCallingConvention(compiler, prototype)
                .getLLVMValue(compiler.getTargetMachine()));
        super.enterExternFunction(context);
    }

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

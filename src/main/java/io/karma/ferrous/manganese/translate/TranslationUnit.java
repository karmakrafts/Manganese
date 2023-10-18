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

import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.Module;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
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

    public TranslationUnit(final Compiler compiler, final String name) {
        super(compiler);
        module = new Module(name);
    }

    @Override
    public void enterFunction(final FunctionContext context) {
        final var parser = new FunctionParser(compiler, scopeStack);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        final var function = parser.getFunction();
        functions.put(function.getInternalName(), function);
        super.enterFunction(context);
    }

    @Override
    public void enterExternFunction(final ExternFunctionContext context) {
        final var prototype = context.protoFunction();
        // @formatter:off
        final var type = TypeUtils.getFunctionType(compiler, scopeStack, prototype)
            .unwrapOrReport(compiler, context.start, CompileStatus.TRANSLATION_ERROR);
        // @formatter:on
        if (type.isEmpty()) {
            return;
        }
        final var name = FunctionUtils.getFunctionName(prototype.functionIdent()).toString();
        final var function = LLVMAddFunction(module.getAddress(), name,
                                             type.get().materialize(compiler.getTargetMachine()));
        if (function == NULL) {
            compiler.reportError(compiler.makeError(context.start, "Could not materialize function"),
                                 CompileStatus.TRANSLATION_ERROR);
            return;
        }
        LLVMSetLinkage(function, LLVMExternalLinkage);
        LLVMSetFunctionCallConv(function, FunctionUtils.getCallingConvention(compiler, prototype).getLlvmType());
        super.enterExternFunction(context);
    }

    public Module getModule() {
        return module;
    }
}

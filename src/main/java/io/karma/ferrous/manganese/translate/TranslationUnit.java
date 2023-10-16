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
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.vanadium.FerrousParser.ExternFunctionContext;

import static org.lwjgl.llvm.LLVMCore.LLVMAddFunction;
import static org.lwjgl.llvm.LLVMCore.LLVMDisposeModule;
import static org.lwjgl.llvm.LLVMCore.LLVMExternalLinkage;
import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;
import static org.lwjgl.llvm.LLVMCore.LLVMGetModuleIdentifier;
import static org.lwjgl.llvm.LLVMCore.LLVMModuleCreateWithNameInContext;
import static org.lwjgl.llvm.LLVMCore.LLVMSetFunctionCallConv;
import static org.lwjgl.llvm.LLVMCore.LLVMSetLinkage;
import static org.lwjgl.llvm.LLVMCore.LLVMSetSourceFileName;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public class TranslationUnit extends ParseAdapter {
    private final long module;
    private boolean isDisposed;

    public TranslationUnit(final Compiler compiler, final String name) {
        super(compiler);

        module = LLVMModuleCreateWithNameInContext(name, LLVMGetGlobalContext());
        if (module == NULL) {
            throw new RuntimeException("Could not allocate module");
        }
        LLVMSetSourceFileName(module, name); // Same as source file name
        Logger.INSTANCE.debugln("Allocated translation unit at 0x%08X", module);
    }

    @Override
    public void enterExternFunction(ExternFunctionContext context) {
        compiler.doOrReport(context, () -> {
            final var prototype = context.protoFunction();
            final var type = TypeUtils.getFunctionType(compiler, prototype);
            final var function = LLVMAddFunction(module,
                                                 FunctionUtils.getFunctionName(prototype.functionIdent()).toString(),
                                                 type.materialize(compiler.getTarget()));
            if (function == NULL) {
                throw new TranslationException(context.start, "Could not create function");
            }
            LLVMSetLinkage(function, LLVMExternalLinkage);
            LLVMSetFunctionCallConv(function, FunctionUtils.getCallingConvention(compiler, prototype).getLlvmType());
        }, CompileStatus.TRANSLATION_ERROR);
        super.enterExternFunction(context);
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        Logger.INSTANCE.debugln("Disposing translation unit at 0x%08X", module);
        LLVMDisposeModule(module);
        isDisposed = true;
    }

    public long getModule() {
        return module;
    }

    public String getModuleName() {
        return LLVMGetModuleIdentifier(module);
    }
}

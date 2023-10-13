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

import io.karma.ferrous.manganese.CompileError;
import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.util.CallingConvention;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.CallConvModContext;
import io.karma.ferrous.vanadium.FerrousParser.ExternFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.FunctionContext;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public class TranslationUnit extends AbstractTranslationUnit {
    private final Compiler compiler;
    private final long module;
    private boolean isDisposed;
    private CallingConvention callingConvention = CallingConvention.CDECL;

    public TranslationUnit(final String name, final Compiler compiler) {
        this.compiler = compiler;
        module = LLVMCore.LLVMModuleCreateWithName(name);
        if (module == NULL) {
            throw new RuntimeException("Could not allocate module");
        }
        LLVMCore.LLVMSetSourceFileName(module, name); // Same as source file name
        Logger.INSTANCE.debugln("Allocated translation unit module at 0x%08X", module);
    }

    @Override
    public void enterFunction(FunctionContext functionContext) {
        final var prototype = functionContext.protoFunction();
        final var ident = prototype.functionIdent();
    }

    @Override
    public void enterExternFunction(ExternFunctionContext externFunctionContext) {
        final var prototype = externFunctionContext.protoFunction();
        final var ident = prototype.functionIdent();
    }

    @Override
    public void enterCallConvMod(CallConvModContext callConvModContext) {
        final var identifier = callConvModContext.IDENT();
        final var name = identifier.getText();
        // @formatter:off
        final var convOption = Arrays.stream(CallingConvention.values())
            .filter(conv -> conv.getText().equals(name))
            .findFirst();
        // @formatter:on
        if (convOption.isEmpty()) {
            final var error = new CompileError(identifier.getSymbol());
            final var message = String.format("'%s' is not a valid calling convention, expected one of the following values", name);
            error.setAdditionalText(Utils.makeSuggestion(message, CallingConvention.EXPECTED_VALUES));
            compiler.reportError(error, CompileStatus.SEMANTIC_ERROR);
            return;
        }
        callingConvention = convOption.get();
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
        LLVMCore.LLVMDisposeModule(module);
        isDisposed = true;
    }

    public long getModule() {
        return module;
    }

    public String getModuleName() {
        return LLVMCore.LLVMGetModuleIdentifier(module);
    }
}

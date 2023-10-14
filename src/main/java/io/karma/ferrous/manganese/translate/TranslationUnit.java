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
import io.karma.ferrous.manganese.ocm.StructureType;
import io.karma.ferrous.manganese.target.CallingConvention;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.CallConvModContext;
import io.karma.ferrous.vanadium.FerrousParser.ExternFunctionContext;

import java.util.Arrays;
import java.util.HashMap;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public class TranslationUnit extends AbstractTranslationUnit {
    private static final CallingConvention DEFAULT_CALL_CONV = CallingConvention.CDECL;
    private final long module;
    private final HashMap<String, StructureType> structures = new HashMap<>();
    private boolean isDisposed;
    private CallingConvention callingConvention = DEFAULT_CALL_CONV;

    public TranslationUnit(final Compiler compiler, final String name) {
        super(compiler);
        module = LLVMModuleCreateWithName(name);
        if (module == NULL) {
            throw new RuntimeException("Could not allocate module");
        }
        LLVMSetSourceFileName(module, name); // Same as source file name
        Logger.INSTANCE.debugln("Allocated translation unit at 0x%08X", module);
    }

    private CallingConvention consumeCallConv() {
        final var result = callingConvention;
        callingConvention = DEFAULT_CALL_CONV;
        return result;
    }

    @Override
    public void enterExternFunction(ExternFunctionContext context) {
        doOrReport(context, () -> {
            final var prototype = context.protoFunction();
            final var type = TypeUtils.getFunctionType(compiler, prototype);
            final var name = FunctionUtils.getFunctionName(prototype.functionIdent());
            final var function = LLVMAddFunction(module, name, type.materialize(compiler.getTarget()));
            if (function == NULL) {
                throw new TranslationException(context.start, "Could not create function");
            }
            LLVMSetLinkage(function, LLVMExternalLinkage);
            LLVMSetFunctionCallConv(function, consumeCallConv().getLlvmType());
        });
    }

    @Override
    public void enterCallConvMod(CallConvModContext context) {
        final var identifier = context.IDENT();
        final var name = identifier.getText();
        // @formatter:off
        final var convOption = Arrays.stream(CallingConvention.values())
            .filter(conv -> conv.getText().equals(name))
            .findFirst();
        // @formatter:on
        if (convOption.isEmpty()) {
            final var error = new CompileError(identifier.getSymbol());
            final var message = String.format("'%s' is not a valid calling convention, expected one of the following values", name);
            error.setAdditionalText(Utils.makeCompilerMessage(message, CallingConvention.EXPECTED_VALUES));
            compiler.reportError(error, CompileStatus.SEMANTIC_ERROR);
            return;
        }
        callingConvention = convOption.get();
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

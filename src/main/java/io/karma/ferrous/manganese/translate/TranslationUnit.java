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
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import io.karma.ferrous.vanadium.FerrousParser.UdtDeclContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.Stack;

import static org.lwjgl.llvm.LLVMCore.LLVMDisposeModule;
import static org.lwjgl.llvm.LLVMCore.LLVMGetModuleIdentifier;
import static org.lwjgl.llvm.LLVMCore.LLVMModuleCreateWithName;
import static org.lwjgl.llvm.LLVMCore.LLVMSetSourceFileName;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public class TranslationUnit extends AbstractTranslationUnit {
    private final long module;
    private final Stack<Scope> scopes = new Stack<>();
    private boolean isDisposed;
    private Token currentToken;
    private CallingConvention callingConvention = CallingConvention.CDECL;

    public TranslationUnit(final Compiler compiler, final String name) {
        super(compiler);
        module = LLVMModuleCreateWithName(name);
        if (module == NULL) {
            throw new RuntimeException("Could not allocate module");
        }
        LLVMSetSourceFileName(module, name); // Same as source file name
        Logger.INSTANCE.debugln("Allocated translation unit module at 0x%08X", module);
    }

    @Override
    public void enterUdtDecl(UdtDeclContext ctx) {
        final var unit = new UDTTranslationUnit(compiler);
        ParseTreeWalker.DEFAULT.walk(unit, ctx);
        // TODO: add UDT to module
        scopes.push(new Scope());
    }

    @Override
    public void exitUdtDecl(UdtDeclContext ctx) {
        scopes.pop();
    }

    @Override
    public void enterFunction(FunctionContext ctx) {
        final var unit = new FunctionTranslationUnit(compiler);
        ParseTreeWalker.DEFAULT.walk(unit, ctx);
        // TODO: add function to module
        scopes.push(new Scope());
    }

    @Override
    public void exitFunction(FunctionContext ctx) {
        scopes.pop();
    }

    @Override
    public void enterExternFunction(ExternFunctionContext ctx) {
        doOrReport(() -> {
            final var prototype = ctx.protoFunction();
            final var returnType = Type.findType(compiler, prototype.type()).orElseThrow();
            // @formatter:off
            final var paramTypes = prototype.functionParamList().children.stream()
                .filter(tok -> tok instanceof TypeContext)
                .map(tok -> Type.findType(compiler, (TypeContext) tok).orElseThrow())
                .toList();
            // @formatter:on
            final var ident = prototype.functionIdent();
        }, CompileStatus.TRANSLATION_ERROR);
    }

    @Override
    public void enterCallConvMod(CallConvModContext ctx) {
        final var identifier = ctx.IDENT();
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

    @Override
    public void visitTerminal(TerminalNode node) {
        currentToken = node.getSymbol();
    }

    public void dispose() {
        if (isDisposed) {
            return;
        }
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

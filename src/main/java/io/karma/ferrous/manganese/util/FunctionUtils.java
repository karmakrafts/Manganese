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

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.ferrous.vanadium.FerrousParser.FunctionIdentContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class FunctionUtils {
    // @formatter:off
    private FunctionUtils() {}
    // @formatter:on

    public static Identifier[] parseParameterNames(final ProtoFunctionContext context) {
        final var paramList = context.functionParamList();
        final var params = paramList.functionParam();
        if (params.isEmpty()) {
            return new Identifier[0];
        }
        // @formatter:off
        final var names = params.stream()
            .map(param -> Identifier.parse(param.ident()))
            .collect(Collectors.toCollection(ArrayList::new));
        // @formatter:on
        final var vaParam = paramList.vaFunctionParam();
        if (vaParam != null) {
            names.add(Identifier.parse(vaParam.ident()));
        }
        return names.toArray(Identifier[]::new);
    }

    public static CallingConvention parseCallingConvention(final CompileContext compileContext,
                                                           final ProtoFunctionContext context) {
        final var convContext = context.callConvMod();
        if (convContext == null) {
            return CallingConvention.CDECL;
        }
        final var identifier = convContext.IDENT();
        final var name = identifier.getText();
        final var conv = CallingConvention.findByText(name);
        if (conv.isEmpty()) {
            final var message = String.format(
                "'%s' is not a valid calling convention, expected one of the following values",
                name);
            final var formattedMessage = KitchenSink.makeCompilerMessage(message, CallingConvention.EXPECTED_VALUES);
            compileContext.reportError(identifier.getSymbol(), formattedMessage, CompileErrorCode.E4000);
            return CallingConvention.CDECL;
        }
        return conv.get();
    }

    public static Identifier parseFunctionName(final FunctionIdentContext context) {
        final var children = context.children;
        if (children.size() == 1) {
            final var text = children.getFirst().getText();
            final var op = Operator.findByText(text);
            if (op.isPresent()) {
                return Identifier.parse(op.get().getFunctionName());
            }
        }
        return Identifier.parse(context.ident());
    }

    public static List<Type> parseParameterTypes(final Compiler compiler, final CompileContext compileContext,
                                                 final ScopeStack scopeStack,
                                                 final @Nullable ProtoFunctionContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return context.functionParamList().functionParam().stream()
            .map(FerrousParser.FunctionParamContext::type)
            .filter(type -> type != null && !type.getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS)))
            .map(type -> TypeUtils.parseType(compiler, compileContext, scopeStack, type))
            .peek(type -> {
                //if(type == BuiltinType.VOID) {
                //    compileContext.reportError(compileContext.makeError());
                //} TODO: ...
            })
            .toList();
        // @formatter:on
    }

    public static FunctionType parseFunctionType(final Compiler compiler, final CompileContext compileContext,
                                                 final ScopeStack scopeStack, final ProtoFunctionContext context) {
        final var type = context.type();
        // @formatter:off
        final var returnType = type == null
            ? BuiltinType.VOID
            : Objects.requireNonNull(TypeUtils.parseType(compiler, compileContext, scopeStack, type));
        // @formatter:on
        final var isVarArg = context.functionParamList().vaFunctionParam() != null;
        final var paramTypes = parseParameterTypes(compiler, compileContext, scopeStack, context);
        return Types.function(returnType,
            paramTypes,
            isVarArg,
            scopeStack::applyEnclosingScopes,
            TokenSlice.from(compileContext, context));
    }
}

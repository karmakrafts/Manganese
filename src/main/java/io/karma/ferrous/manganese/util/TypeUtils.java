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
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.translate.TypeParser;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.FunctionParamContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeListContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class TypeUtils {
    // @formatter:off
    private TypeUtils() {}
    // @formatter:on

    public static List<Type> getTypes(final Compiler compiler, final CompileContext compileContext, final ScopeStack scopeStack,
                                      final TypeListContext context) {
        // @formatter:off
        return context.type().stream()
            .map(ctx -> getType(compiler, compileContext, scopeStack, ctx))
            .toList();
        // @formatter:on
    }

    public static Type getType(final Compiler compiler, final CompileContext compileContext, final ScopeStack scopeStack, final TypeContext context) {
        final TypeParser unit = new TypeParser(compiler, compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(unit, context);
        return unit.getType();
    }

    public static List<Type> getParameterTypes(final Compiler compiler, final CompileContext compileContext, final ScopeStack scopeStack,
                                               final ProtoFunctionContext context) {
        // @formatter:off
        return context.functionParamList().functionParam().stream()
            .map(FunctionParamContext::type)
            .filter(type -> type != null && !type.getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS)))
            .map(type -> getType(compiler, compileContext, scopeStack, type))
            .toList();
        // @formatter:on
    }

    public static FunctionType getFunctionType(final Compiler compiler, final CompileContext compileContext, final ScopeStack scopeStack,
                                               final ProtoFunctionContext context) {
        final var type = context.type();
        final var returnType = type == null ? BuiltinType.VOID : getType(compiler, compileContext, scopeStack, type);
        final var isVarArg = context.functionParamList().vaFunctionParam() != null;
        final var paramTypes = TypeUtils.getParameterTypes(compiler, compileContext, scopeStack, context);
        return Types.function(returnType, paramTypes, isVarArg, scopeStack::applyEnclosingScopes);
    }
}

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

import io.karma.ferrous.manganese.analyze.GenericExpressionAnalyzer;
import io.karma.ferrous.manganese.analyze.TypeAnalyzer;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.constant.TypeConstant;
import io.karma.ferrous.manganese.ocm.generic.GenericConstraint;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class TypeUtils {
    // @formatter:off
    private TypeUtils() {}
    // @formatter:on

    public static List<GenericParameter> getGenericParams(final Compiler compiler, final CompileContext compileContext,
                                                          final ScopeStack scopeStack,
                                                          final @Nullable GenericParamListContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        final var params = context.genericParam();
        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }
        final var result = new ArrayList<GenericParameter>();
        for (final var param : params) {
            final var name = Utils.getIdentifier(param.ident());
            final var expr = param.genericExpr();
            final var defaultType = getType(compiler, compileContext, scopeStack, param.type());
            var constraints = GenericConstraint.TRUE;
            if (expr != null) {
                final var analyzer = new GenericExpressionAnalyzer(compiler, compileContext);
                ParseTreeWalker.DEFAULT.walk(analyzer, expr);
                constraints = analyzer.getConstraints();
            }
            result.add(new GenericParameter(name, constraints, new TypeConstant(defaultType)));
        }
        return result;
    }

    public static List<Type> getTypes(final Compiler compiler, final CompileContext compileContext,
                                      final ScopeStack scopeStack, final @Nullable TypeListContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return context.type().stream()
            .map(ctx -> Objects.requireNonNull(getType(compiler, compileContext, scopeStack, ctx)))
            .toList();
        // @formatter:on
    }

    public static @Nullable Type getType(final Compiler compiler, final CompileContext compileContext,
                                         final ScopeStack scopeStack, final @Nullable TypeContext context) {
        if (context == null) {
            return null;
        }
        final TypeAnalyzer unit = new TypeAnalyzer(compiler, compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(unit, context);
        return unit.getType();
    }

    public static List<Type> getParameterTypes(final Compiler compiler, final CompileContext compileContext,
                                               final ScopeStack scopeStack,
                                               final @Nullable ProtoFunctionContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return context.functionParamList().functionParam().stream()
            .map(FunctionParamContext::type)
            .filter(type -> type != null && !type.getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS)))
            .map(type -> Objects.requireNonNull(getType(compiler, compileContext, scopeStack, type)))
            .toList();
        // @formatter:on
    }

    public static FunctionType getFunctionType(final Compiler compiler, final CompileContext compileContext,
                                               final ScopeStack scopeStack, final ProtoFunctionContext context) {
        final var type = context.type();
        // @formatter:off
        final var returnType = type == null
            ? BuiltinType.VOID
            : Objects.requireNonNull(getType(compiler, compileContext, scopeStack, type));
        // @formatter:on
        final var isVarArg = context.functionParamList().vaFunctionParam() != null;
        final var paramTypes = TypeUtils.getParameterTypes(compiler, compileContext, scopeStack, context);
        return Types.function(returnType, paramTypes, isVarArg, scopeStack::applyEnclosingScopes);
    }
}

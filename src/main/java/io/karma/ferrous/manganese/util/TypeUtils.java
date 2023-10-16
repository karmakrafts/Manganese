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

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ocm.FunctionType;
import io.karma.ferrous.manganese.ocm.Type;
import io.karma.ferrous.manganese.ocm.Types;
import io.karma.ferrous.manganese.scope.ScopeStack;
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.manganese.translate.TypeParser;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.FunctionParamContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TypeUtils {
    // @formatter:off
    private TypeUtils() {}
    // @formatter:on

    /**
     * Look up the given type in the current context. This means unrolling
     * the scope stack in reverse to find the first match of an unqualified type name.
     *
     * @param compiler   The compiler instance.
     * @param scopeStack The scope stack of the current context.
     * @param context    The actual type context AST node.
     * @return An optional with a type if one is found, otherwise empty.
     */
    public static Optional<Type> getType(final Compiler compiler, final ScopeStack scopeStack,
                                         final TypeContext context) {
        final TypeParser unit = new TypeParser(compiler, scopeStack);
        ParseTreeWalker.DEFAULT.walk(unit, context);
        if (!compiler.getStatus().isRecoverable()) {
            return Optional.empty();
        }
        return Optional.of(scopeStack.applyEnclosingScopes(unit.getType()));
    }

    public static List<Type> getParameterTypes(final Compiler compiler, final ScopeStack scopeStack,
                                               final ProtoFunctionContext context) throws TranslationException {
        // @formatter:off
        return context.functionParamList().functionParam().stream()
            .map(FunctionParamContext::functionParamType)
            .filter(type -> !type.getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS)))
            .map(type -> getType(compiler, scopeStack, type.type())
                .orElseThrow(() -> new TranslationException(context.start, "Unknown function parameter type '%s'", type.getText())))
            .toList();
        // @formatter:on
    }

    public static FunctionType getFunctionType(final Compiler compiler, final ScopeStack scopeStack,
                                               final ProtoFunctionContext context) throws TranslationException {
        final var type = context.type();
        // @formatter:off
        final var returnType = getType(compiler, scopeStack, type)
            .orElseThrow(() -> new TranslationException(context.start, "Unknown function return type '%s'", type.getText()));
        // @formatter:on
        final var params = context.functionParamList().functionParam();
        var isVarArg = false;

        if (!params.isEmpty()) {
            final var paramType = params.getLast().functionParamType();
            final var nodes = paramType.children;
            isVarArg = nodes.size() == 1 && nodes.get(0).getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS));
        }

        final var paramTypes = TypeUtils.getParameterTypes(compiler, scopeStack, context);
        return Types.function(returnType, paramTypes, isVarArg);
    }
}

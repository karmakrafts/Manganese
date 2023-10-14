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
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.FunctionParamContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TypeUtils {
    // @formatter:off
    private TypeUtils() {}
    // @formatter:on

    public static List<Type> getParameterTypes(final Compiler compiler,
                                               final ProtoFunctionContext context) throws TranslationException {
        // @formatter:off
        return context.functionParamList().functionParam().stream()
            .map(FunctionParamContext::functionParamType)
            .filter(type -> !type.getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS)))
            .map(type -> Type.findType(compiler, type.type())
                .orElseThrow(() -> new TranslationException(context.start, "Unknown function parameter type '%s'", type.getText())))
            .toList();
        // @formatter:on
    }

    public static FunctionType getFunctionType(final Compiler compiler,
                                               final ProtoFunctionContext context) throws TranslationException {
        final var type = context.type();
        // @formatter:off
        final var returnType = Type.findType(compiler, type)
            .orElseThrow(() -> new TranslationException(context.start, "Unknown function return type '%s'", type.getText()));
        // @formatter:on
        final var params = context.functionParamList().functionParam();
        var isVarArg = false;

        if (!params.isEmpty()) {
            final var paramType = params.getLast().functionParamType();
            final var nodes = paramType.children;
            isVarArg = nodes.size() == 1 && nodes.get(0).getText().equals(TokenUtils.getLiteral(FerrousLexer.KW_VAARGS));
        }

        final var paramTypes = TypeUtils.getParameterTypes(compiler, context);
        return new FunctionType(returnType, paramTypes, isVarArg);
    }
}

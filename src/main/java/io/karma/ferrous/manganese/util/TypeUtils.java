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
import io.karma.ferrous.manganese.type.FunctionType;
import io.karma.ferrous.manganese.type.Type;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;

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

    public static List<Type> getParameterTypes(final Compiler compiler, final ProtoFunctionContext context) {
        // @formatter:off
        return context.functionParamList().children.stream()
            .filter(tok -> tok instanceof TypeContext)
            .map(tok -> Type.findType(compiler, (TypeContext) tok).orElseThrow())
            .toList();
        // @formatter:on
    }

    public static Optional<FunctionType> getFunctionType(final Compiler compiler, final ProtoFunctionContext context) {
        try {
            final var returnType = Type.findType(compiler, context.type()).orElseThrow();
            final var params = context.functionParamList().functionParam();
            var isVarArg = false;

            if (!params.isEmpty()) {
                final var paramType = params.getLast().functionParamType();
                final var nodes = paramType.children;
                final var keyword = FerrousLexer.VOCABULARY.getLiteralName(FerrousLexer.KW_VAARGS);
                isVarArg = nodes.size() == 1 && nodes.get(0).getText().equals(keyword);
            }

            final var paramTypes = TypeUtils.getParameterTypes(compiler, context);
            return Optional.of(new FunctionType(returnType, paramTypes, isVarArg));
        }
        catch (Exception error) {
            return Optional.empty();
        }
    }
}

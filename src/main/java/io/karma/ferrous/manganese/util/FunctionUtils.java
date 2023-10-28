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
import io.karma.ferrous.vanadium.FerrousParser.FunctionIdentContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.ArrayList;
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

    public static Identifier[] getParameterNames(final ProtoFunctionContext context) {
        final var paramList = context.functionParamList();
        final var params = paramList.functionParam();
        if (params.isEmpty()) {
            return new Identifier[0];
        }
        // @formatter:off
        final var names = params.stream()
            .map(param -> Utils.getIdentifier(param.ident()))
            .collect(Collectors.toCollection(ArrayList::new));
        // @formatter:on
        final var vaParam = paramList.vaFunctionParam();
        if (vaParam != null) {
            names.add(Utils.getIdentifier(vaParam.ident()));
        }
        return names.toArray(Identifier[]::new);
    }

    public static CallingConvention getCallingConvention(final CompileContext compileContext,
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
            final var formattedMessage = Utils.makeCompilerMessage(message, CallingConvention.EXPECTED_VALUES);
            compileContext.reportError(compileContext.makeError(identifier.getSymbol(),
                formattedMessage,
                CompileErrorCode.E5000));
            return CallingConvention.CDECL;
        }
        return conv.get();
    }

    public static Identifier getFunctionName(final FunctionIdentContext context) {
        final var children = context.children;
        if (children.size() == 1) {
            final var text = children.getFirst().getText();
            final var op = Operator.findByText(text);
            if (op.isPresent()) {
                return Identifier.parse(op.get().getFunctionName());
            }
        }
        return Utils.getIdentifier(context.ident());
    }
}

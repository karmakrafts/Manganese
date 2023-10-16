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

import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.util.CallingConvention;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public final class FunctionParser extends ParseAdapter {
    private Identifier identifier;
    private FunctionType type;
    private CallingConvention callConv;

    public FunctionParser(final Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        compiler.doOrReport(context, () -> {
            identifier = FunctionUtils.getFunctionName(context.functionIdent());
            type = TypeUtils.getFunctionType(compiler, context);
            callConv = FunctionUtils.getCallingConvention(compiler, context);
        }, CompileStatus.TRANSLATION_ERROR);
        super.enterProtoFunction(context);
    }

    public Function getFunction() {
        return new Function(identifier, type);
    }
}

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

package io.karma.ferrous.manganese.compiler.pass;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import org.apiguardian.api.API;

import java.util.concurrent.ExecutorService;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class EmitPass implements CompilePass {
    @Override
    public void run(final Compiler compiler, final CompileContext compileContext, final Module module,
                    final ExecutorService executor) {
        Profiler.INSTANCE.push();
        compileContext.walkParseTree(new ParseListenerImpl(compiler, compileContext, module));
        Profiler.INSTANCE.pop();
    }

    private static final class ParseListenerImpl extends ParseAdapter {
        private final Module module;

        public ParseListenerImpl(final Compiler compiler, final CompileContext compileContext, final Module module) {
            super(compiler, compileContext);
            this.module = module;
        }

        @Override
        public void enterProtoFunction(final ProtoFunctionContext context) {
            final var function = getFunction(context);
            if (function == null) {
                compileContext.reportError(context.functionIdent().start, CompileErrorCode.E5000);
                return;
            }
            function.materialize(compileContext, module, compiler.getTargetMachine());
            final var body = function.getBody();
            if (body != null) {
                pushScope(body);
            }
        }
    }
}

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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.parser.FunctionParser;
import io.karma.ferrous.manganese.parser.ParseAdapter;
import io.karma.ferrous.vanadium.FerrousParser.FunctionContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;

import java.util.concurrent.ExecutorService;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionDefinitionPass implements CompilePass {
    @Override
    public void run(final CompileContext compileContext, final Module module, final ExecutorService executor) {
        final var profiler = compileContext.getCompiler().getProfiler();
        profiler.push();
        compileContext.walkParseTree(new ParseListenerImpl(compileContext));
        profiler.pop();
    }

    private static final class ParseListenerImpl extends ParseAdapter {
        public ParseListenerImpl(final CompileContext compileContext) {
            super(compileContext);
        }

        @Override
        public void enterFunction(final FunctionContext context) {
            final var function = getFunction(context.protoFunction());
            if (function == null) {
                super.enterFunction(context);
                return;
            }
            final var bodyContext = context.functionBody();
            if (bodyContext == null) {
                super.enterFunction(context);
                return; // TODO: handle arrow functions
            }

            super.enterFunction(context);
            ParseTreeWalker.DEFAULT.walk(new FunctionParser(compileContext, function), bodyContext);
            popScope();
            pushScope(function.getBody());
        }
    }
}

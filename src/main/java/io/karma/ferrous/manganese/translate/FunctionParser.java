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

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.vanadium.FerrousParser.FunctionBodyContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class FunctionParser extends ParseAdapter {
    private final Function function;

    public FunctionParser(final Compiler compiler, final CompileContext compileContext, final Function function) {
        super(compiler, compileContext);
        this.function = function;
    }

    @Override
    public void enterFunctionBody(final FunctionBodyContext context) {
        if (function.getBody() != null) {
            return;
        }
        final var analyzer = new FunctionBodyParser(compiler, compileContext, function.getType());
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        function.createBody(analyzer.getStatements().toArray(Statement[]::new));
        super.enterFunctionBody(context);
    }

    public Function getFunction() {
        return function;
    }
}

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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.parser.FunctionParser;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.ferrous.vanadium.FerrousParser.FunctionContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Alexander Hinze
 * @since 07/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class PostAnalyzer extends ParseAdapter {
    private final LinkedHashMap<Function, LinkedHashMap<Identifier, LetStatement>> locals = new LinkedHashMap<>();
    private FunctionParser functionParser;

    public PostAnalyzer(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
    }

    @Override
    public void enterFunction(final FunctionContext context) {
        final var function = getFunction(context.protoFunction());
        if (function == null) {
            return; // TODO: log warning/error?
        }
        final var bodyContext = context.functionBody();
        if (bodyContext == null) {
            return; // TODO: handle arrow functions
        }

        super.enterFunction(context);

        functionParser = new FunctionParser(compiler, compileContext, function);
        ParseTreeWalker.DEFAULT.walk(functionParser, bodyContext);
        locals.put(function, functionParser.getLocals());

        popScope();
        pushScope(function.getBody());
    }

    public Map<Identifier, LetStatement> getLocalsFor(final Function function) {
        if (functionParser.getFunction() == function) {
            final var map = functionParser.getLocals();
            if (map != null) {
                return map;
            }
            return Collections.emptyMap();
        }
        final var map = locals.get(function);
        if (map != null) {
            return map;
        }
        return Collections.emptyMap();
    }

    public LetStatement findLocalIn(final Function function, final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(getLocalsFor(function), name, scopeName);
    }
}

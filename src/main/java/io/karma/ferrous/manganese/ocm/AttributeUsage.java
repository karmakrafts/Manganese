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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
import io.karma.ferrous.manganese.parser.AttributeUsageParser;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.vanadium.FerrousParser.AttribUsageContext;
import io.karma.ferrous.vanadium.FerrousParser.AttributeListContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public record AttributeUsage(UserDefinedType attribute, Map<Identifier, Expression> values) {
    public static AttributeUsage parse(final Compiler compiler, final CompileContext compileContext,
                                       final ScopeStack capturedScopeStack, final AttribUsageContext context) {
        final var parser = new AttributeUsageParser(compiler, compileContext, capturedScopeStack);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        return parser.getUsage();
    }

    public static List<AttributeUsage> parse(final Compiler compiler, final CompileContext compileContext,
                                             final ScopeStack capturedScopeStack, final AttributeListContext context) {
        final var usageContexts = context.attribUsage();
        if (usageContexts == null || usageContexts.isEmpty()) {
            return Collections.emptyList();
        }
        final var usages = new ArrayList<AttributeUsage>();
        for (final var usageContext : usageContexts) {
            usages.add(parse(compiler, compileContext, capturedScopeStack, usageContext));
        }
        return usages;
    }
}

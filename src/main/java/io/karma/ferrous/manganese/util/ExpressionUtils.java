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

import io.karma.ferrous.manganese.analyze.ExpressionAnalyzer;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.vanadium.FerrousParser.ExprContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
public final class ExpressionUtils {
    // @formatter:off
    private ExpressionUtils() {}
    // @formatter:on

    public static @Nullable Expression parseExpression(final Compiler compiler, final CompileContext compileContext,
                                                       final ExprContext context) {
        final var analyzer = new ExpressionAnalyzer(compiler, compileContext);
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        final var expression = analyzer.getExpression();
        if (expression == null) {
            compileContext.reportError(compileContext.makeError(context.start, CompileErrorCode.E2001));
        }
        return expression;
    }
}

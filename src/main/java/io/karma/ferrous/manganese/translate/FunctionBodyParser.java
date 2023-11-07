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
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.FunctionType;
import io.karma.ferrous.manganese.util.ExpressionUtils;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ReturnStatementContext;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionBodyParser extends ParseAdapter {
    private final FunctionType functionType;
    private final ArrayList<Statement> statements = new ArrayList<>();

    public FunctionBodyParser(final Compiler compiler, final CompileContext compileContext,
                              final FunctionType functionType) {
        super(compiler, compileContext);
        this.functionType = functionType;
    }

    @Override
    public void enterReturnStatement(final ReturnStatementContext context) {
        final var exprContext = context.expr();
        if (exprContext != null) {
            final var expr = ExpressionUtils.parseExpression(compiler, compileContext, exprContext);
            if (expr == null) {
                return;
            }
            final var returnType = functionType.getReturnType();
            final var exprType = expr.getType();
            if (!returnType.canAccept(exprType)) {
                final var message = Utils.makeCompilerMessage(String.format("%s cannot be assigned to %s",
                    exprType,
                    returnType));
                compileContext.reportError(context.start, message, CompileErrorCode.E3006);
                return;
            }
            statements.add(new ReturnStatement(expr, TokenSlice.from(compileContext, context)));
            super.enterReturnStatement(context);
            return;
        }
        statements.add(new ReturnStatement(TokenSlice.from(compileContext, context)));
        super.enterReturnStatement(context);
    }

    public @Nullable ArrayList<Statement> getStatements() {
        return statements;
    }
}

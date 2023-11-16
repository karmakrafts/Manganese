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

package io.karma.ferrous.manganese.parser;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.ocm.statement.ReturnStatement;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.ExpressionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.LetStatementContext;
import io.karma.ferrous.vanadium.FerrousParser.ReturnStatementContext;
import io.karma.ferrous.vanadium.FerrousParser.StatementContext;
import org.apiguardian.api.API;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class StatementParser extends ParseAdapter {
    private final Type returnType;
    private final ArrayList<Statement> statements = new ArrayList<>();
    private final ScopeStack capturedScopeStack;
    private final Object parent;

    public StatementParser(final Compiler compiler, final CompileContext compileContext, final Type returnType,
                           final ScopeStack capturedScopeStack, final Object parent) {
        super(compiler, compileContext);
        this.returnType = returnType;
        this.capturedScopeStack = capturedScopeStack;
        this.parent = parent;
    }

    private void addStatement(final Statement statement) {
        if (parent instanceof Function function && statement instanceof LetStatement let) {
            compileContext.getOrCreateModuleData().getLocalsFor(function).put(let.getQualifiedName(), let);
        }
        statements.add(statement);
    }

    @Override
    public void enterStatement(final StatementContext context) {
        final var exprContext = context.expr();
        if (exprContext != null) {
            final var expr = ExpressionUtils.parseExpression(compiler,
                compileContext,
                capturedScopeStack,
                exprContext,
                parent);
            if (expr == null) {
                compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                return;
            }
            expr.setResultDiscarded(true); // Statement means we always discard our result
            statements.add(expr);
        }
        super.enterStatement(context);
    }

    @Override
    public void enterLetStatement(final LetStatementContext context) {
        try {
            final var name = Identifier.parse(context.ident());
            final var typeContext = context.type();
            final var exprContext = context.expr();
            final var isMutable = context.KW_MUT() != null;
            final var storageMods = KitchenSink.parseStorageMods(context.storageMod());
            Type type = null;
            if (typeContext != null) {
                type = Types.parse(compiler, compileContext, scopeStack, typeContext);
                if (type == null || !type.isComplete()) {
                    compileContext.reportError(typeContext.start, CompileErrorCode.E3002);
                    return;
                }
                if (context.QMK() != null) {
                    if (!isMutable) {
                        compileContext.reportError(context.QMK().getSymbol(), CompileErrorCode.E4010);
                        return;
                    }
                    addStatement(new LetStatement(name,
                        type,
                        null,
                        true,
                        false,
                        storageMods,
                        TokenSlice.from(compileContext, context)));
                    return;
                }
                if (exprContext == null) {
                    addStatement(new LetStatement(name,
                        type,
                        type.makeDefaultValue(),
                        isMutable,
                        true,
                        storageMods,
                        TokenSlice.from(compileContext, context)));
                    return;
                }
            }
            if (exprContext == null) {
                compileContext.reportError(context.start, CompileErrorCode.E4006);
                return;
            }
            final var expr = ExpressionUtils.parseExpression(compiler,
                compileContext,
                capturedScopeStack,
                exprContext,
                parent);
            if (expr == null) {
                compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                return;
            }
            if (type == null) {
                type = expr.getType(); // Deduce variable kind from expression
            }
            if (expr instanceof NullConstant nll) {
                nll.setContextualType(type); // Give null contextual type information about our variable
            }
            addStatement(new LetStatement(name,
                type,
                expr,
                isMutable,
                true,
                storageMods,
                TokenSlice.from(compileContext, context)));
        }
        finally {
            super.enterLetStatement(context);
        }
    }

    @Override
    public void enterReturnStatement(final ReturnStatementContext context) {
        try {
            final var exprContext = context.expr();
            if (exprContext != null) {
                final var expr = ExpressionUtils.parseExpression(compiler,
                    compileContext,
                    capturedScopeStack,
                    exprContext,
                    parent);
                if (expr == null) {
                    return;
                }
                final var exprType = expr.getType();
                if (!returnType.canAccept(exprType)) {
                    final var message = KitchenSink.makeCompilerMessage(String.format("%s cannot be assigned to %s",
                        exprType,
                        returnType));
                    compileContext.reportError(context.start, message, CompileErrorCode.E3006);
                    return;
                }
                statements.add(new ReturnStatement(expr, TokenSlice.from(compileContext, context)));
                return;
            }
            statements.add(new ReturnStatement(TokenSlice.from(compileContext, context)));
        }
        finally {
            super.enterReturnStatement(context);
        }
    }

    public List<Statement> getStatements() {
        return statements == null ? Collections.emptyList() : statements;
    }
}

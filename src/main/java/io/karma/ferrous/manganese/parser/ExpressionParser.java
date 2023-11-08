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
import io.karma.ferrous.manganese.ocm.constant.BoolConstant;
import io.karma.ferrous.manganese.ocm.constant.IntConstant;
import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.constant.RealConstant;
import io.karma.ferrous.manganese.ocm.expr.BinaryExpression;
import io.karma.ferrous.manganese.ocm.expr.CallExpression;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.expr.UnaryExpression;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.*;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ExpressionParser extends ParseAdapter {
    private Expression expression;

    public ExpressionParser(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
    }

    private IntConstant parseIntConstant(final BuiltinType type, final TerminalNode node) {
        final var suffix = type.getName().toString();
        var text = node.getText();
        if (text.endsWith(suffix)) {
            text = text.substring(0, text.length() - suffix.length());
        }
        return new IntConstant(type, Long.parseLong(text), TokenSlice.from(compileContext, node));
    }

    private RealConstant parseRealConstant(final BuiltinType type, final TerminalNode node) {
        final var suffix = type.getName().toString();
        var text = node.getText();
        if (text.endsWith(suffix)) {
            text = text.substring(0, text.length() - suffix.length());
        }
        return new RealConstant(type, Double.parseDouble(text), TokenSlice.from(compileContext, node));
    }

    private StringConstant parseStringConstant(final ParserRuleContext context) {
        return new StringConstant("", TokenSlice.from(compileContext, context));
    }

    private @Nullable UnaryExpression parseUnaryExpression(final ExprContext context, final List<ParseTree> children) {
        final var opContextOpt = children.stream().filter(TerminalNode.class::isInstance).findFirst();
        if (opContextOpt.isEmpty()) {
            return null;
        }
        final var opContext = opContextOpt.get();
        final var opText = opContext.getText();
        final var opOpt = Operator.findByText(opText);
        if (opOpt.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E5001);
            return null;
        }
        var op = opOpt.get();
        if (children.indexOf(opContext) > 0) {
            op = switch(op) { // @formatter:off
                case PRE_DEC        -> Operator.DEC;
                case PRE_INC        -> Operator.INC;
                case PRE_INV_ASSIGN -> Operator.INV_ASSIGN;
                default             -> op;
            }; // @formatter:on
        }
        final var exprOpt = children.stream().filter(ExprContext.class::isInstance).findFirst();
        if (exprOpt.isEmpty()) {
            compileContext.reportError(context.start, CompileErrorCode.E5003);
            return null;
        }
        return new UnaryExpression(op,
            ExpressionUtils.parseExpression(compiler, compileContext, exprOpt.get()),
            TokenSlice.from(compileContext, context));
    }

    private @Nullable BinaryExpression parseBinaryExpression(final ExprContext context,
                                                             final List<ParseTree> children) {
        final var opContextOpt = children.stream().filter(TerminalNode.class::isInstance).findFirst();
        if (opContextOpt.isEmpty()) {
            return null;
        }
        final var opText = opContextOpt.get().getText();
        final var op = Operator.findByText(opText);
        if (op.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E5002);
            return null;
        }
        final var lhs = ExpressionUtils.parseExpression(compiler, compileContext, children.get(0));
        if (lhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E5003);
            return null;
        }
        final var rhs = ExpressionUtils.parseExpression(compiler, compileContext, children.get(2));
        if (rhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E5003);
            return null;
        }
        return new BinaryExpression(op.get(), lhs, rhs, TokenSlice.from(compileContext, context));
    }

    @Override
    public void enterExpr(final ExprContext context) {
        if (expression != null) {
            return;
        }
        final var children = context.children;
        switch (children.size()) {
            case 2:
                expression = parseUnaryExpression(context, children);
                break;
            case 3:
                expression = parseBinaryExpression(context, children);
                break;
        }
        super.enterExpr(context);
    }

    @Override
    public void enterCallExpr(final CallExprContext context) {
        if (expression != null) {
            return;
        }
        final var qualifiedIdentContext = context.qualifiedIdent();
        Identifier name;
        if (qualifiedIdentContext != null) {
            name = Utils.getIdentifier(qualifiedIdentContext);
        }
        else {
            name = Utils.getIdentifier(context.ident());
        }
        final var scopeName = scopeStack.getScopeName();
        final var args = ExpressionUtils.parseExpressions(compiler, compileContext, context.exprList());
        final var paramTypes = args.stream().map(Expression::getType).toArray(Type[]::new);
        final var function = compileContext.getPreAnalyzer().findFunctionInScope(name, scopeName, null, paramTypes);
        expression = new CallExpression(function,
            TokenSlice.from(compileContext, context),
            args.toArray(Expression[]::new));
        super.enterCallExpr(context);
    }

    @Override
    public void enterSintLiteral(final SintLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_I8();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I8, node);
            return;
        }
        node = context.LITERAL_I16();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I16, node);
            return;
        }
        node = context.LITERAL_I32();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I32, node);
            return;
        }
        node = context.LITERAL_I64();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.I64, node);
            return;
        }
        node = context.LITERAL_ISIZE();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.ISIZE, node);
        }
        super.enterSintLiteral(context);
    }

    @Override
    public void enterUintLiteral(final UintLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_U8();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U8, node);
            return;
        }
        node = context.LITERAL_U16();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U16, node);
            return;
        }
        node = context.LITERAL_U32();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U32, node);
            return;
        }
        node = context.LITERAL_U64();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.U64, node);
            return;
        }
        node = context.LITERAL_USIZE();
        if (node != null) {
            expression = parseIntConstant(BuiltinType.USIZE, node);
        }
        super.enterUintLiteral(context);
    }

    @Override
    public void enterFloatLiteral(final FloatLiteralContext context) {
        if (expression != null) {
            return;
        }
        var node = context.LITERAL_F32();
        if (node != null) {
            expression = parseRealConstant(BuiltinType.F32, node);
            return;
        }
        node = context.LITERAL_F64();
        if (node != null) {
            expression = parseRealConstant(BuiltinType.F64, node);
        }
        super.enterFloatLiteral(context);
    }

    @Override
    public void enterBoolLiteral(final BoolLiteralContext context) {
        if (expression != null) {
            return;
        }
        expression = new BoolConstant(context.KW_TRUE() != null, TokenSlice.from(compileContext, context));
        super.enterBoolLiteral(context);
    }

    @Override
    public void enterLiteral(final LiteralContext context) {
        if (expression != null) {
            return;
        }
        if (context.KW_NULL() != null) {
            expression = NullConstant.INSTANCE;
        }
        super.enterLiteral(context);
    }

    public @Nullable Expression getExpression() {
        return expression;
    }
}

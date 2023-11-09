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
import io.karma.ferrous.manganese.ocm.constant.*;
import io.karma.ferrous.manganese.ocm.expr.*;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.FunctionReference;
import io.karma.ferrous.manganese.ocm.function.FunctionResolver;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.util.*;
import io.karma.ferrous.vanadium.FerrousLexer;
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
    private final ScopeStack capturedScopeStack;
    private final Object parent;
    private Expression expression;

    public ExpressionParser(final Compiler compiler, final CompileContext compileContext,
                            final ScopeStack capturedScopeStack, final @Nullable Object parent) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
        this.parent = parent;
    }

    private void setExpression(final ParserRuleContext context, final @Nullable Expression expression) {
        if (expression == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return;
        }
        this.expression = expression;
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
        final var opOpt = Operator.findByText(opText, false);
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
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        return new UnaryExpression(op,
            ExpressionUtils.parseExpression(compiler, compileContext, scopeStack, exprOpt.get(), parent),
            TokenSlice.from(compileContext, context));
    }

    private @Nullable BinaryExpression parseBinaryExpression(final ExprContext context,
                                                             final List<ParseTree> children) {
        final var opContextOpt = children.stream().filter(TerminalNode.class::isInstance).findFirst();
        if (opContextOpt.isEmpty()) {
            return null;
        }
        final var opText = opContextOpt.get().getText();
        final var op = Operator.findByText(opText, true);
        if (op.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E5002);
            return null;
        }
        final var lhs = ExpressionUtils.parseExpression(compiler, compileContext, scopeStack, children.get(0), parent);
        if (lhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        final var rhs = ExpressionUtils.parseExpression(compiler, compileContext, scopeStack, children.get(2), parent);
        if (rhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        return new BinaryExpression(op.get(), lhs, rhs, TokenSlice.from(compileContext, context));
    }

    private @Nullable CallExpression parseCallExpr(final ExprContext exprContext, final List<ParseTree> children) {
        final var expr = ExpressionUtils.parseExpression(compiler,
            compileContext,
            capturedScopeStack,
            exprContext,
            parent);
        if (!(expr instanceof ReferenceExpression refExpr)) {
            return null;
        }
        final var genericListOpt = children.stream().filter(GenericListContext.class::isInstance).findFirst();
        if (genericListOpt.isPresent()) {
            // TODO: parse generic list
        }
        final var argsContext = exprContext.exprList();
        final var args = ExpressionUtils.parseExpressions(compiler,
            compileContext,
            capturedScopeStack,
            argsContext,
            parent);
        final var ref = refExpr.getReference();
        if (!(ref instanceof FunctionReference funRef)) {
            return null;
        }
        funRef.setContextualParamTypes(args.stream().map(Expression::getType).toArray(Type[]::new));
        final var function = funRef.resolve();
        if (function == null) {
            compileContext.reportError(refExpr.getTokenSlice().getFirstToken(), CompileErrorCode.E4009);
            return null;
        }
        return new CallExpression(function, TokenSlice.from(compileContext, exprContext));
    }

    private @Nullable CallExpression parseNamedArgCallExpr(final List<ParseTree> children) {
        final var genericListOpt = children.stream().filter(GenericListContext.class::isInstance).findFirst();
        if (genericListOpt.isPresent()) {
            // TODO: parse generic list
        }
        return null;
    }

    private @Nullable IndexExpression parseIndexExpr(final List<ParseTree> children) {
        final var genericListOpt = children.stream().filter(GenericListContext.class::isInstance).findFirst();
        if (genericListOpt.isPresent()) {
            // TODO: parse generic list
        }
        return null;
    }

    private @Nullable ReferenceExpression parseReference(final ParserRuleContext context) {
        final var name = Identifier.parse(context);
        if (parent instanceof Function function) {
            final var local = compileContext.getPostAnalyzer().findLocalIn(function,
                name,
                capturedScopeStack.getScopeName());
            if (local != null) {
                return new ReferenceExpression(local, false, TokenSlice.from(compileContext, context));
            }
        }
        // TODO: implement parsing of field references
        final var preAnalyzer = compileContext.getPreAnalyzer();
        if (!preAnalyzer.functionExistsInScope(name, capturedScopeStack.getScopeName())) {
            compileContext.reportError(context.start, CompileErrorCode.E4007);
            return null;
        }
        final FunctionResolver resolver = paramTypes -> preAnalyzer.findFunctionInScope(name,
            capturedScopeStack.getScopeName(),
            paramTypes);
        return new ReferenceExpression(new FunctionReference(resolver),
            false,
            TokenSlice.from(compileContext, context));
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        if (expression != null) {
            return;
        }
        setExpression(context, parseReference(context));
        super.enterQualifiedIdent(context);
    }

    @Override
    public void enterIdent(final IdentContext context) {
        if (expression != null) {
            return;
        }
        setExpression(context, parseReference(context));
        super.enterIdent(context);
    }

    @Override
    public void enterExpr(final ExprContext context) {
        if (expression != null) {
            return;
        }
        try {
            final var children = context.children;
            final var numChildren = children.size();
            if (numChildren == 2) {
                setExpression(context, parseUnaryExpression(context, children));
                return;
            }
            if (numChildren >= 3) {
                // Binary expressions
                if (numChildren == 3 && KitchenSink.areTypesAssignable(children,
                    ExprContext.class,
                    TerminalNode.class,
                    ExprContext.class)) {
                    setExpression(context, parseBinaryExpression(context, children));
                    return;
                }
                if (children.get(0) instanceof ExprContext exprContext) {
                    final var expr = ExpressionUtils.parseExpression(compiler,
                        compileContext,
                        scopeStack,
                        exprContext,
                        parent);
                    if (expr == null) {
                        compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                        return;
                    }
                    // Call with named arguments
                    if (KitchenSink.containsAssignableTypeSequence(children,
                        TerminalNode.class,
                        NamedExprListContext.class,
                        TerminalNode.class)) {
                        setExpression(context, parseNamedArgCallExpr(children));
                        return;
                    }
                    // Call with sequential arguments or indexing
                    if (KitchenSink.containsAssignableTypeSequence(children,
                        TerminalNode.class,
                        ExprListContext.class,
                        TerminalNode.class)) {
                        final var openParen = children.stream().filter(TerminalNode.class::isInstance).findFirst().orElseThrow();
                        // We know it is a function call because of regular parens
                        if (openParen.getText().equals(TokenUtils.getLiteral(FerrousLexer.L_PAREN))) {
                            setExpression(context, parseCallExpr(exprContext, children));
                            return;
                        }
                        // Otherwise it has to be an indexing expression
                        setExpression(context, parseIndexExpr(children));
                        return;
                    }
                    // Call without arguments
                    if (KitchenSink.containsAssignableTypeSequence(children, TerminalNode.class, TerminalNode.class)) {
                        final var openParen = children.stream().filter(TerminalNode.class::isInstance).findFirst().orElseThrow();
                        if (!openParen.getText().equals(TokenUtils.getLiteral(FerrousLexer.L_PAREN))) {
                            compileContext.reportError(context.start, CompileErrorCode.E4008);
                            return;
                        }
                        setExpression(context, parseCallExpr(exprContext, children));
                    }
                }
            }
        }
        finally {
            super.enterExpr(context);
        }
    }

    @Override
    public void enterSintLiteral(final SintLiteralContext context) {
        if (expression != null) {
            return;
        }
        try {
            var node = context.LITERAL_I8();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.I8, node));
                return;
            }
            node = context.LITERAL_I16();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.I16, node));
                return;
            }
            node = context.LITERAL_I32();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.I32, node));
                return;
            }
            node = context.LITERAL_I64();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.I64, node));
                return;
            }
            node = context.LITERAL_ISIZE();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.ISIZE, node));
            }
        }
        finally {
            super.enterSintLiteral(context);
        }
    }

    @Override
    public void enterUintLiteral(final UintLiteralContext context) {
        if (expression != null) {
            return;
        }
        try {
            var node = context.LITERAL_U8();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.U8, node));
                return;
            }
            node = context.LITERAL_U16();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.U16, node));
                return;
            }
            node = context.LITERAL_U32();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.U32, node));
                return;
            }
            node = context.LITERAL_U64();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.U64, node));
                return;
            }
            node = context.LITERAL_USIZE();
            if (node != null) {
                setExpression(context, parseIntConstant(BuiltinType.USIZE, node));
            }
        }
        finally {
            super.enterUintLiteral(context);
        }
    }

    @Override
    public void enterFloatLiteral(final FloatLiteralContext context) {
        if (expression != null) {
            return;
        }
        try {
            var node = context.LITERAL_F32();
            if (node != null) {
                setExpression(context, parseRealConstant(BuiltinType.F32, node));
                return;
            }
            node = context.LITERAL_F64();
            if (node != null) {
                setExpression(context, parseRealConstant(BuiltinType.F64, node));
            }
        }
        finally {
            super.enterFloatLiteral(context);
        }
    }

    @Override
    public void enterBoolLiteral(final BoolLiteralContext context) {
        if (expression != null) {
            return;
        }
        setExpression(context, new BoolConstant(context.KW_TRUE() != null, TokenSlice.from(compileContext, context)));
        super.enterBoolLiteral(context);
    }

    @Override
    public void enterLiteral(final LiteralContext context) {
        if (expression != null) {
            return;
        }
        if (context.KW_NULL() != null) {
            setExpression(context, new NullConstant(TokenSlice.from(compileContext, context)));
        }
        super.enterLiteral(context);
    }

    @Override
    public void enterSimpleStringLiteral(final SimpleStringLiteralContext context) {
        if (expression != null) {
            return;
        }
        setExpression(context, parseStringConstant(context));
        super.enterSimpleStringLiteral(context);
    }

    @Override
    public void enterMultilineStringLiteral(final MultilineStringLiteralContext context) {
        if (expression != null) {
            return;
        }
        setExpression(context, parseStringConstant(context));
        super.enterMultilineStringLiteral(context);
    }

    public @Nullable Expression getExpression() {
        return expression;
    }
}

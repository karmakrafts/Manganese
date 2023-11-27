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
import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.constant.*;
import io.karma.ferrous.manganese.ocm.expr.*;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.FunctionReference;
import io.karma.ferrous.manganese.ocm.function.FunctionResolver;
import io.karma.ferrous.manganese.ocm.function.UnresolvedFunctionReference;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.util.*;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
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
    private boolean isAtEnd;

    public ExpressionParser(final Compiler compiler, final CompileContext compileContext,
                            final ScopeStack capturedScopeStack, final Object parent) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
        this.parent = parent;
    }

    private void setExpression(final ParserRuleContext context, final @Nullable Expression expression) {
        isAtEnd = true;
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
        final var opOpt = Operator.findByText(opText);
        if (opOpt.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E4013);
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
        final var expr = ExpressionUtils.parseExpression(compiler,
            compileContext,
            capturedScopeStack,
            exprOpt.get(),
            parent);
        if (expr == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        return new UnaryExpression(op, expr, TokenSlice.from(compileContext, context));
    }

    private @Nullable BinaryExpression parseBinaryExpression(final ExprContext context,
                                                             final List<ParseTree> children) {
        final var opContextOpt = children.stream().filter(TerminalNode.class::isInstance).findFirst();
        if (opContextOpt.isEmpty()) {
            return null;
        }
        final var opText = opContextOpt.get().getText();
        final var opOpt = Operator.findByText(opText);
        if (opOpt.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E4014);
            return null;
        }
        final var lhs = ExpressionUtils.parseExpression(compiler,
            compileContext,
            capturedScopeStack,
            children.get(0),
            parent);
        if (lhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        final var rhs = ExpressionUtils.parseExpression(compiler,
            compileContext,
            capturedScopeStack,
            children.get(2),
            parent);
        if (rhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        // Various checks for assignments and swaps
        final var op = opOpt.get();
        if (op.isAssignment() && lhs instanceof ReferenceExpression lhsRef) {
            lhsRef.setIsWrite(true); // If this is a LHS reference in a binary op, we do a write
            switch (lhsRef.getReference()) {
                case ValueStorage lhsStorage -> {
                    if (!lhsStorage.isMutable()) {
                        compileContext.reportError(context.start, CompileErrorCode.E4011);
                        return null;
                    }
                    if (op == Operator.SWAP) {
                        if (!(rhs instanceof ReferenceExpression rhsRef) || !(rhsRef.getReference() instanceof ValueStorage rhsStorage)) {
                            compileContext.reportError(context.start, CompileErrorCode.E4012);
                            return null;
                        }
                        if (!rhsStorage.isMutable()) {
                            compileContext.reportError(context.start, CompileErrorCode.E4011);
                            return null;
                        }
                    }
                }
                default -> {
                }
            }
        }
        return new BinaryExpression(opOpt.get(), lhs, rhs, TokenSlice.from(compileContext, context));
    }

    private @Nullable CallExpression parseCallExpr(final ExprContext exprContext,
                                                   final @Nullable ExprListContext argsContext,
                                                   final List<ParseTree> children) {
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
        final var args = ExpressionUtils.parseExpressions(compiler,
            compileContext,
            capturedScopeStack,
            argsContext,
            parent);
        final var function = switch (refExpr.getReference()) {
            case UnresolvedFunctionReference unresolvedRef -> {
                unresolvedRef.setContextualParamTypes(args.stream().map(Expression::getType).toList());
                yield unresolvedRef.get();
            }
            case FunctionReference ref -> ref.get();
            default -> throw new IllegalStateException("Unsupported reference type");
        };
        if (function == null) {
            compileContext.reportError(refExpr.getTokenSlice().getFirstToken(), CompileErrorCode.E4009);
            return null;
        }
        return new CallExpression(function,
            TokenSlice.from(compileContext, exprContext),
            args.toArray(Expression[]::new));
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

    private @Nullable ReferenceExpression parseReference(final ParserRuleContext context, final Object parent) {
        if (!(parent instanceof Scoped scoped)) {
            return null;
        }
        final var moduleData = compileContext.getOrCreateModuleData();
        final var scopeName = scoped.getScopeName();
        final var name = Identifier.parse(context);
        Logger.INSTANCE.debugln("Looking for reference to %s", name);
        if (parent instanceof Function function) {
            final var param = function.getParameters().stream().filter(p -> p.getName().equals(name)).findFirst();
            if (param.isPresent()) {
                return new ReferenceExpression(param.get(), false, TokenSlice.from(compileContext, context));
            }
            final var local = moduleData.findLocalIn(function, name, scopeName);
            if (local != null) {
                return new ReferenceExpression(local, false, TokenSlice.from(compileContext, context));
            }
        }
        if (parent instanceof ReferenceExpression parentRef) {
            // FIXME: FIXMEEEE
            //if (parentRef.getReference() instanceof FieldStorageProvider provider) {
            //    final var storage = provider.getFieldValue(name);
            //    if (storage != null) {
            //        return new ReferenceExpression(storage, false, TokenSlice.from(compileContext, context));
            //    }
            //}
        }
        if (!moduleData.functionExists(name, scopeName)) {
            compileContext.reportError(context.start, CompileErrorCode.E4007);
            return null;
        }
        final FunctionResolver resolver = paramTypes -> moduleData.findFunction(name, scopeName, paramTypes);
        return new ReferenceExpression(new UnresolvedFunctionReference(resolver),
            false,
            TokenSlice.from(compileContext, context));
    }

    private @Nullable ReferenceExpression parseBinaryReference(final ParserRuleContext context,
                                                               final List<ParseTree> children) {
        final var expr = ExpressionUtils.parseExpression(compiler,
            compileContext,
            capturedScopeStack,
            children.get(0),
            parent);
        if (expr == null) {
            return null;
        }
        final var opContext = children.get(1);
        final var opOpt = Operator.findByText(opContext.getText());
        if (opOpt.isEmpty()) {
            return null;
        }
        return parseReference((IdentContext) children.get(2), expr);
    }

    @Override
    public void enterIdent(final IdentContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseReference(context, parent));
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseReference(context, parent));
    }

    @Override
    public void enterExpr(final ExprContext context) {
        if (isAtEnd) {
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
                if (numChildren == 3) {
                    // Binary expressions
                    if (KitchenSink.areTypesAssignable(children,
                        ExprContext.class,
                        TerminalNodeImpl.class,
                        ExprContext.class)) {
                        setExpression(context, parseBinaryExpression(context, children));
                        return;
                    }
                    // References
                    if (KitchenSink.areTypesAssignable(children,
                        ExprContext.class,
                        TerminalNodeImpl.class,
                        IdentContext.class)) {
                        setExpression(context, parseBinaryReference(context, children));
                        return;
                    }
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
                        TerminalNodeImpl.class,
                        NamedExprListContext.class,
                        TerminalNodeImpl.class)) {
                        setExpression(context, parseNamedArgCallExpr(children));
                        return;
                    }
                    // Call with sequential arguments or indexing
                    if (KitchenSink.containsAssignableTypeSequence(children,
                        TerminalNodeImpl.class,
                        ExprListContext.class,
                        TerminalNodeImpl.class)) {
                        final var openParen = children.stream().filter(TerminalNodeImpl.class::isInstance).findFirst().orElseThrow();
                        // We know it is a function call because of regular parens
                        if (openParen.getText().equals(TokenUtils.getLiteral(FerrousLexer.L_PAREN))) {
                            setExpression(context, parseCallExpr(exprContext, context.exprList(), children));
                            return;
                        }
                        // Otherwise it has to be an indexing expression
                        setExpression(context, parseIndexExpr(children));
                        return;
                    }
                    // Call without arguments
                    if (KitchenSink.containsAssignableTypeSequence(children,
                        TerminalNodeImpl.class,
                        TerminalNodeImpl.class)) {
                        final var openParen = children.stream().filter(TerminalNodeImpl.class::isInstance).findFirst().orElseThrow();
                        if (!openParen.getText().equals(TokenUtils.getLiteral(FerrousLexer.L_PAREN))) {
                            compileContext.reportError(context.start, CompileErrorCode.E4008);
                            return;
                        }
                        setExpression(context, parseCallExpr(exprContext, context.exprList(), children));
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
        if (isAtEnd) {
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
        if (isAtEnd) {
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
        if (isAtEnd) {
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
        if (isAtEnd) {
            return;
        }
        setExpression(context, new BoolConstant(context.KW_TRUE() != null, TokenSlice.from(compileContext, context)));
        super.enterBoolLiteral(context);
    }

    @Override
    public void enterLiteral(final LiteralContext context) {
        if (isAtEnd) {
            return;
        }
        if (context.KW_NULL() != null) {
            setExpression(context, new NullConstant(TokenSlice.from(compileContext, context)));
        }
        super.enterLiteral(context);
    }

    @Override
    public void enterSimpleStringLiteral(final SimpleStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
        super.enterSimpleStringLiteral(context);
    }

    @Override
    public void enterMlStringLiteral(final MlStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
        super.enterMlStringLiteral(context);
    }

    @Override
    public void enterCmlStringLiteral(final CmlStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
        super.enterCmlStringLiteral(context);
    }

    public @Nullable Expression getExpression() {
        return expression;
    }
}

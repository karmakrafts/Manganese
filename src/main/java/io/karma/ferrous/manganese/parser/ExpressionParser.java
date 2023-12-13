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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.constant.*;
import io.karma.ferrous.manganese.ocm.expr.*;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.ocm.function.FunctionReference;
import io.karma.ferrous.manganese.ocm.function.FunctionResolver;
import io.karma.ferrous.manganese.ocm.function.UnresolvedFunctionReference;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.Scoped;
import io.karma.ferrous.manganese.ocm.type.IntType;
import io.karma.ferrous.manganese.ocm.type.RealType;
import io.karma.ferrous.manganese.ocm.type.SizeType;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.*;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.kommons.function.Functions;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
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

    public ExpressionParser(final CompileContext compileContext, final ScopeStack capturedScopeStack,
                            final @Nullable Object parent) {
        super(compileContext);
        this.capturedScopeStack = capturedScopeStack;
        this.parent = parent;
    }

    public static List<Expression> parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                         final @Nullable ExprListContext context, final @Nullable Object parent) {
        if (context == null) {
            return Collections.emptyList();
        }
        final var contexts = context.expr();
        if (contexts == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return contexts.stream()
            .map(exprContext -> {
                final var expression = parse(compileContext, scopeStack, exprContext, parent);
                if(expression == null) {
                    compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                }
                return expression;
            })
            .toList();
        // @formatter:on
    }

    public static @Nullable Expression parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                             final @Nullable ParseTree context, final @Nullable Object parent) {
        if (context == null) {
            return null;
        }
        final var parser = new ExpressionParser(compileContext, scopeStack, parent);
        ParseTreeWalker.DEFAULT.walk(parser, context);
        return parser.expression;
    }

    public static @Nullable Pair<Identifier, Expression> parseNamed(final CompileContext compileContext,
                                                                    final ScopeStack scopeStack,
                                                                    final @Nullable NamedExprContext context,
                                                                    final @Nullable Object parent) {
        if (context == null) {
            return null;
        }
        final var parser = new ExpressionParser(compileContext, scopeStack, parent);
        ParseTreeWalker.DEFAULT.walk(parser, context.expr());
        return Pair.of(Identifier.parse(context.ident()), parser.expression);
    }

    public static List<Pair<Identifier, Expression>> parseNamed(final CompileContext compileContext,
                                                                final ScopeStack scopeStack,
                                                                final @Nullable NamedExprListContext context,
                                                                final @Nullable Object parent) {
        if (context == null) {
            return Collections.emptyList();
        }
        final var contexts = context.namedExpr();
        if (contexts == null) {
            return Collections.emptyList();
        }
        // @formatter:off
        return contexts.stream()
            .map(exprContext -> {
                final var expression = parseNamed(compileContext, scopeStack, exprContext, parent);
                if(expression == null) {
                    compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                }
                return expression;
            })
            .toList();
        // @formatter:on
    }

    private void setExpression(final ParserRuleContext context, final @Nullable Expression expression) {
        isAtEnd = true;
        if (expression == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return;
        }
        this.expression = expression;
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
        final var opOpt = Operator.unaryByText(opText);
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
        final var expr = parse(compileContext, capturedScopeStack, exprOpt.get(), parent);
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
        final var opOpt = Operator.binaryByText(opText);
        if (opOpt.isEmpty()) {
            compileContext.reportError(context.start, opText, CompileErrorCode.E4014);
            return null;
        }
        final var lhs = parse(compileContext, capturedScopeStack, children.get(0), parent);
        if (lhs == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        final var rhs = parse(compileContext, capturedScopeStack, children.get(2), parent);
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
        final var expr = parse(compileContext, capturedScopeStack, exprContext, parent);
        if (!(expr instanceof ReferenceExpression refExpr)) {
            return null;
        }
        final var genericListOpt = children.stream().filter(GenericListContext.class::isInstance).findFirst();
        if (genericListOpt.isPresent()) {
            // TODO: parse generic list
        }
        final var args = parse(compileContext, capturedScopeStack, argsContext, parent);
        final var function = switch (refExpr.getReference()) {
            case UnresolvedFunctionReference unresolvedRef -> {
                unresolvedRef.setContextualParamTypes(args.stream().map(e -> e.getType(compileContext.getCompiler().getTargetMachine())).toList());
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

    private @Nullable SubscriptExpression parseSubscriptExpr(final List<ParseTree> children) {
        final var genericListOpt = children.stream().filter(GenericListContext.class::isInstance).findFirst();
        if (genericListOpt.isPresent()) {
            // TODO: parse generic list
        }
        return null;
    }

    private @Nullable TernaryExpression parseTernaryExpr(final ParserRuleContext context,
                                                         final List<ParseTree> children) {
        final var conditionContext = children.getFirst();
        final var condition = parse(compileContext, capturedScopeStack, conditionContext, parent);
        if (condition == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        final var trueValue = parse(compileContext, capturedScopeStack, children.get(2), parent);
        if (trueValue == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        final var falseValue = parse(compileContext, capturedScopeStack, children.getLast(), parent);
        if (falseValue == null) {
            compileContext.reportError(context.start, CompileErrorCode.E2001);
            return null;
        }
        return new TernaryExpression(condition, trueValue, falseValue, TokenSlice.from(compileContext, context));
    }

    private @Nullable ReferenceExpression parseReference(final ParserRuleContext context, final Object parent) {
        if (!(parent instanceof Scoped scoped)) {
            return null;
        }
        final var moduleData = compileContext.getOrCreateModuleData();
        final var scopeName = scoped.getScopeName();
        final var name = Identifier.parse(context);
        Logger.INSTANCE.debugln(STR."Looking for reference to \{name}");
        if (parent instanceof Function function) {
            final var param = function.getParamStorage(name);
            if (param != null) {
                return new ReferenceExpression(param, false, TokenSlice.from(compileContext, context));
            }
            final var local = moduleData.findLocalIn(function, name, scopeName);
            if (local != null) {
                return new ReferenceExpression(local, false, TokenSlice.from(compileContext, context));
            }
        }
        else if (parent instanceof ReferenceExpression refExpr) {
            return switch (refExpr.getReference()) {
                case ValueStorage storage ->
                    new ReferenceExpression(storage.getField(name), false, TokenSlice.from(compileContext, context));
                default -> null;
            };
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

    private @Nullable ReferenceExpression parseBinaryReference(final List<ParseTree> children) {
        final var expr = parse(compileContext, capturedScopeStack, children.get(0), parent);
        if (expr == null) {
            return null;
        }
        final var opContext = children.get(1);
        final var opOpt = Operator.binaryByText(opContext.getText());
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
                    setExpression(context, parseBinaryReference(children));
                    return;
                }
            }
            if (children.get(0) instanceof ExprContext exprContext) {
                final var expr = parse(compileContext, scopeStack, exprContext, parent);
                if (expr == null) {
                    compileContext.reportError(exprContext.start, CompileErrorCode.E2001);
                    return;
                }
                // Ternary expressions
                if (KitchenSink.containsAssignableTypeSequence(children,
                    ExprContext.class,
                    TerminalNodeImpl.class,
                    ExprContext.class,
                    TerminalNodeImpl.class,
                    ExprContext.class)) {
                    setExpression(context, parseTernaryExpr(context, children));
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
                    setExpression(context, parseSubscriptExpr(children));
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

    @Override
    public void enterSintLiteral(final SintLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        final var targetMachine = compileContext.getCompiler().getTargetMachine();
        final var sizeContext = context.LITERAL_ISIZE();
        // Size type
        if (sizeContext != null) {
            final var stringValue = sizeContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_ISIZE), "");
            if (targetMachine.getPointerSize() <= 8) {
                setExpression(context,
                    new IntConstant(SizeType.ISIZE,
                        Long.parseLong(stringValue),
                        TokenSlice.from(compileContext, context)));
                return;
            }
            setExpression(context,
                new BigIntConstant(SizeType.ISIZE,
                    new BigInteger(stringValue),
                    TokenSlice.from(compileContext, context)));
            return;
        }
        // Otherwise variable-width int literal which defaults to i32
        final var stringValue = context.LITERAL_INT().getText();
        if (stringValue.contains("i")) {
            final var parts = stringValue.split("i");
            final var width = Integer.parseInt(parts[1]);
            final var type = Types.integer(width, false, Functions.castingIdentity());
            if (width <= 64) {
                setExpression(context,
                    new IntConstant(type, Long.parseLong(parts[0]), TokenSlice.from(compileContext, context)));
                return;
            }
            setExpression(context,
                new BigIntConstant(type, new BigInteger(parts[0]), TokenSlice.from(compileContext, context)));
            return;
        }
        setExpression(context,
            new IntConstant(IntType.I32, Long.parseLong(stringValue), TokenSlice.from(compileContext, context)));
    }

    @Override
    public void enterUintLiteral(final UintLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        final var targetMachine = compileContext.getCompiler().getTargetMachine();
        final var sizeContext = context.LITERAL_USIZE();
        // Size type
        if (sizeContext != null) {
            final var stringValue = sizeContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_USIZE), "");
            if (targetMachine.getPointerSize() <= 8) {
                setExpression(context,
                    new IntConstant(SizeType.USIZE,
                        Long.parseUnsignedLong(stringValue),
                        TokenSlice.from(compileContext, context)));
                return;
            }
            setExpression(context,
                new BigIntConstant(SizeType.USIZE,
                    new BigInteger(stringValue),
                    TokenSlice.from(compileContext, context)));
            return;
        }
        // Otherwise variable-width int literal which defaults to i32
        final var stringValue = context.LITERAL_UINT().getText();
        if (stringValue.contains("u")) {
            final var parts = stringValue.split("u");
            final var width = Integer.parseInt(parts[1]);
            final var type = Types.integer(width, false, Functions.castingIdentity());
            if (width <= 64) {
                setExpression(context,
                    new IntConstant(type, Long.parseUnsignedLong(parts[0]), TokenSlice.from(compileContext, context)));
                return;
            }
            setExpression(context,
                new BigIntConstant(type, new BigInteger(parts[0]), TokenSlice.from(compileContext, context)));
        }
        // TODO: error here..
    }

    @Override
    public void enterFloatLiteral(final FloatLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        final var halfContext = context.LITERAL_F16();
        if (halfContext != null) {
            final var stringValue = halfContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_F16), "");
            final var value = Double.parseDouble(stringValue);
            setExpression(context, new RealConstant(RealType.F16, value, TokenSlice.from(compileContext, context)));
            return;
        }
        final var singleContext = context.LITERAL_F32();
        if (singleContext != null) {
            final var stringValue = singleContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_F32), "");
            final var value = Double.parseDouble(stringValue);
            setExpression(context, new RealConstant(RealType.F32, value, TokenSlice.from(compileContext, context)));
            return;
        }
        final var doubleContext = context.LITERAL_F64();
        if (doubleContext != null) {
            final var stringValue = doubleContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_F64), "");
            final var value = Double.parseDouble(stringValue);
            setExpression(context, new RealConstant(RealType.F64, value, TokenSlice.from(compileContext, context)));
            return;
        }
        final var quadContext = context.LITERAL_F128();
        if (quadContext != null) {
            final var stringValue = quadContext.getText().replace(TokenUtils.getLiteral(FerrousLexer.KW_F128), "");
            final var value = new BigDecimal(stringValue);
            setExpression(context, new BigRealConstant(RealType.F128, value, TokenSlice.from(compileContext, context)));
        }
    }

    @Override
    public void enterBoolLiteral(final BoolLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, new BoolConstant(context.KW_TRUE() != null, TokenSlice.from(compileContext, context)));
    }

    @Override
    public void enterLiteral(final LiteralContext context) {
        if (isAtEnd) {
            return;
        }
        if (context.KW_NULL() != null) {
            setExpression(context, new NullConstant(TokenSlice.from(compileContext, context)));
        }
    }

    @Override
    public void enterSimpleStringLiteral(final SimpleStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
    }

    @Override
    public void enterMlStringLiteral(final MlStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
    }

    @Override
    public void enterCmlStringLiteral(final CmlStringLiteralContext context) {
        if (isAtEnd) {
            return;
        }
        setExpression(context, parseStringConstant(context));
    }

    public @Nullable Expression getExpression() {
        return expression;
    }
}

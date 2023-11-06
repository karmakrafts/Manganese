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

package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.scope.ScopeType;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.ferrous.vanadium.FerrousParserListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
@API(status = Status.INTERNAL)
public abstract class ParseAdapter implements FerrousParserListener {
    protected final Compiler compiler;
    protected final CompileContext compileContext;
    protected ScopeStack scopeStack = new ScopeStack();
    protected Scope lastScope;

    protected ParseAdapter(final Compiler compiler, final CompileContext compileContext) {
        this.compiler = compiler;
        this.compileContext = compileContext;
        scopeStack.push(DefaultScope.GLOBAL);
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public CompileContext getCompileContext() {
        return compileContext;
    }

    public ScopeStack getScopeStack() {
        return scopeStack;
    }

    protected void pushScope(final ScopeType type, final Identifier name) {
        scopeStack.push(new DefaultScope(type, name));
    }

    protected Scope popScope() {
        return lastScope = scopeStack.pop();
    }

    // @formatter:off
    @Override
    public void enterBinaryExpr(BinaryExprContext binaryExprContext) {}

    @Override
    public void exitBinaryExpr(BinaryExprContext binaryExprContext) {}

    @Override
    public void enterUnaryExpr(UnaryExprContext unaryExprContext) {}

    @Override
    public void exitUnaryExpr(UnaryExprContext unaryExprContext) {}

    @Override
    public void enterDestructureStatement(DestructureStatementContext destructureStatementContext) {}

    @Override
    public void exitDestructureStatement(DestructureStatementContext destructureStatementContext) {}

    @Override
    public void enterInferredParamList(InferredParamListContext inferredParamListContext) {}

    @Override
    public void exitInferredParamList(InferredParamListContext inferredParamListContext) {}

    @Override
    public void enterPropertyGetter(PropertyGetterContext propertyGetterContext) {}

    @Override
    public void exitPropertyGetter(PropertyGetterContext propertyGetterContext) {}

    @Override
    public void enterPropertySetter(PropertySetterContext propertySetterContext) {}

    @Override
    public void exitPropertySetter(PropertySetterContext propertySetterContext) {}

    @Override
    public void enterProperty(PropertyContext propertyContext) {}

    @Override
    public void exitProperty(PropertyContext propertyContext) {}

    @Override
    public void enterInlinePropertyBody(InlinePropertyBodyContext inlinePropertyBodyContext) {}

    @Override
    public void exitInlinePropertyBody(InlinePropertyBodyContext inlinePropertyBodyContext) {}

    @Override
    public void enterPropertyBody(PropertyBodyContext propertyBodyContext) {}

    @Override
    public void exitPropertyBody(PropertyBodyContext propertyBodyContext) {}

    @Override
    public void enterNamedExpr(NamedExprContext namedExprContext) {}

    @Override
    public void exitNamedExpr(NamedExprContext namedExprContext) {}

    @Override
    public void enterNamedExprList(NamedExprListContext namedExprListContext) {}

    @Override
    public void exitNamedExprList(NamedExprListContext namedExprListContext) {}

    @Override
    public void enterTypeAlias(TypeAliasContext typeAliasContext) {}

    @Override
    public void exitTypeAlias(TypeAliasContext typeAliasContext) {}

    @Override
    public void enterVaFunctionParam(VaFunctionParamContext vaFunctionParamContext) {}

    @Override
    public void exitVaFunctionParam(VaFunctionParamContext vaFunctionParamContext) {}

    @Override
    public void enterLambdaExpr(LambdaExprContext lambdaExprContext) {}

    @Override
    public void exitLambdaExpr(LambdaExprContext lambdaExprContext) {}

    @Override
    public void enterRefIdent(RefIdentContext refIdentContext) {}

    @Override
    public void exitRefIdent(RefIdentContext refIdentContext) {}

    @Override
    public void enterTupleType(TupleTypeContext tupleTypeContext) {}

    @Override
    public void exitTupleType(TupleTypeContext tupleTypeContext) {}

    @Override
    public void enterFunctionType(FunctionTypeContext functionTypeContext) {}

    @Override
    public void exitFunctionType(FunctionTypeContext functionTypeContext) {}

    @Override
    public void enterFile(final FileContext context) {
        pushScope(ScopeType.FILE, Identifier.parse(Objects.requireNonNull(compileContext.getCurrentModuleName())));
    }

    @Override
    public void exitFile(final FileContext context) {
        popScope();
    }

    @Override
    public void enterModuleFile(final ModuleFileContext context) {
        pushScope(ScopeType.MODULE_FILE, Identifier.parse(Objects.requireNonNull(compileContext.getCurrentModuleName())));
    }

    @Override
    public void exitModuleFile(final ModuleFileContext context) {
        popScope();
    }

    @Override
    public void enterModule(ModuleContext moduleContext) {}

    @Override
    public void exitModule(ModuleContext moduleContext) {}

    @Override
    public void enterModBlock(final ModBlockContext context) {
        final var qualifiedIdent = context.qualifiedIdent();
        if(qualifiedIdent != null) {
            pushScope(ScopeType.MODULE, Utils.getIdentifier(qualifiedIdent));
            return;
        }
        pushScope(ScopeType.MODULE, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitModBlock(final ModBlockContext context) {
        popScope();
    }

    @Override
    public void enterModUseStatement(ModUseStatementContext modUseStatementContext) {}

    @Override
    public void exitModUseStatement(ModUseStatementContext modUseStatementContext) {}

    @Override
    public void enterUseTypeList(UseTypeListContext useTypeListContext) {}

    @Override
    public void exitUseTypeList(UseTypeListContext useTypeListContext) {}

    @Override
    public void enterUseType(UseTypeContext useTypeContext) {}

    @Override
    public void exitUseType(UseTypeContext useTypeContext) {}

    @Override
    public void enterSourceFile(SourceFileContext sourceFileContext) {}

    @Override
    public void exitSourceFile(SourceFileContext sourceFileContext) {}

    @Override
    public void enterDecl(DeclContext declContext) {}

    @Override
    public void exitDecl(DeclContext declContext) {}

    @Override
    public void enterUseStatement(UseStatementContext useStatementContext) {}

    @Override
    public void exitUseStatement(UseStatementContext useStatementContext) {}

    @Override
    public void enterUseList(UseListContext useListContext) {}

    @Override
    public void exitUseList(UseListContext useListContext) {}

    @Override
    public void enterUdt(UdtContext udtDeclContext) {}

    @Override
    public void exitUdt(UdtContext udtDeclContext) {}

    @Override
    public void enterEnumClassBody(EnumClassBodyContext context) {}

    @Override
    public void exitEnumClassBody(EnumClassBodyContext context) {}

    @Override
    public void enterEnumClass(EnumClassContext context) {
        pushScope(ScopeType.ENUM_CLASS, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitEnumClass(EnumClassContext context) {
        popScope();
    }

    @Override
    public void enterClass(ClassContext context) {
        pushScope(ScopeType.CLASS, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitClass(ClassContext context) {
        popScope();
    }

    @Override
    public void enterClassBody(ClassBodyContext classBodyContext) {}

    @Override
    public void exitClassBody(ClassBodyContext classBodyContext) {}

    @Override
    public void enterEnumBody(EnumBodyContext enumBodyContext) {}

    @Override
    public void exitEnumBody(EnumBodyContext enumBodyContext) {}

    @Override
    public void enterEnum(EnumContext context) {
        pushScope(ScopeType.ENUM, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitEnum(EnumContext enumContext) {
        popScope();
    }

    @Override
    public void enterEnumConstantList(EnumConstantListContext enumConstantListContext) {}

    @Override
    public void exitEnumConstantList(EnumConstantListContext enumConstantListContext) {}

    @Override
    public void enterEnumConstant(EnumConstantContext enumConstantContext) {}

    @Override
    public void exitEnumConstant(EnumConstantContext enumConstantContext) {}

    @Override
    public void enterStruct(StructContext context) {
        pushScope(ScopeType.STRUCT, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitStruct(StructContext context) {
        popScope();
    }

    @Override
    public void enterInterfaceBody(InterfaceBodyContext interfaceBodyContext) {}

    @Override
    public void exitInterfaceBody(InterfaceBodyContext interfaceBodyContext) {}

    @Override
    public void enterInterface(InterfaceContext context) {
        pushScope(ScopeType.INTERFACE, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitInterface(InterfaceContext context) {
        popScope();
    }

    @Override
    public void enterAttribBody(AttribBodyContext attribBodyContext) {}

    @Override
    public void exitAttribBody(AttribBodyContext attribBodyContext) {}

    @Override
    public void enterAttrib(final AttribContext context) {
        pushScope(ScopeType.ATTRIBUTE, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitAttrib(final AttribContext context) {
        popScope();
    }

    @Override
    public void enterTrait(final TraitContext context) {
        pushScope(ScopeType.TRAIT, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitTrait(final TraitContext context) {
        popScope();
    }

    @Override
    public void enterAttributeList(AttributeListContext attributeListContext) {}

    @Override
    public void exitAttributeList(AttributeListContext attributeListContext) {}

    @Override
    public void enterAttribUsage(AttribUsageContext attribUsageContext) {}

    @Override
    public void exitAttribUsage(AttribUsageContext attribUsageContext) {}

    @Override
    public void enterField(FieldContext fieldContext) {}

    @Override
    public void exitField(FieldContext fieldContext) {}

    @Override
    public void enterConstructor(final ConstructorContext context) {
        pushScope(ScopeType.CONSTRUCTOR, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitConstructor(final ConstructorContext context) {
        popScope();
    }

    @Override
    public void enterThisCall(ThisCallContext thisCallContext) {}

    @Override
    public void exitThisCall(ThisCallContext thisCallContext) {}

    @Override
    public void enterSuperCall(SuperCallContext superCallContext) {}

    @Override
    public void exitSuperCall(SuperCallContext superCallContext) {}

    @Override
    public void enterDestructor(final DestructorContext context) {
        pushScope(ScopeType.DESTRUCTOR, Utils.getIdentifier(context.ident()));
    }

    @Override
    public void exitDestructor(final DestructorContext context) {
        popScope();
    }

    @Override
    public void enterStatement(StatementContext statementContext) {}

    @Override
    public void exitStatement(StatementContext statementContext) {}

    @Override
    public void enterPanicStatement(PanicStatementContext panicStatementContext) {}

    @Override
    public void exitPanicStatement(PanicStatementContext panicStatementContext) {}

    @Override
    public void enterReturnStatement(ReturnStatementContext returnStatementContext) {}

    @Override
    public void exitReturnStatement(ReturnStatementContext returnStatementContext) {}

    @Override
    public void enterWhenExpr(final WhenExprContext context) {
        pushScope(ScopeType.WHEN, Identifier.EMPTY);
    }

    @Override
    public void exitWhenExpr(final WhenExprContext context) {
        popScope();
    }

    @Override
    public void enterWhenBranch(WhenBranchContext whenBranchContext) {}

    @Override
    public void exitWhenBranch(WhenBranchContext whenBranchContext) {}

    @Override
    public void enterDefaultWhenBranch(DefaultWhenBranchContext defaultWhenBranchContext) {}

    @Override
    public void exitDefaultWhenBranch(DefaultWhenBranchContext defaultWhenBranchContext) {}

    @Override
    public void enterWhenBranchBody(WhenBranchBodyContext whenBranchBodyContext) {}

    @Override
    public void exitWhenBranchBody(WhenBranchBodyContext whenBranchBodyContext) {}

    @Override
    public void enterLoop(final LoopContext context) {
        pushScope(ScopeType.LOOP, Identifier.EMPTY);
    }

    @Override
    public void exitLoop(final LoopContext context) {
        popScope();
    }

    @Override
    public void enterWhileBody(final WhileBodyContext context) {
        pushScope(ScopeType.WHILE, Identifier.EMPTY);
    }

    @Override
    public void exitWhileBody(final WhileBodyContext context) {
        popScope();
    }

    @Override
    public void enterWhileLoop(WhileLoopContext context) {}

    @Override
    public void exitWhileLoop(WhileLoopContext context) {}

    @Override
    public void enterSimpleWhileLoop(SimpleWhileLoopContext simpleWhileLoopContext) {}

    @Override
    public void exitSimpleWhileLoop(SimpleWhileLoopContext simpleWhileLoopContext) {}

    @Override
    public void enterDoWhileLoop(DoWhileLoopContext doWhileLoopContext) {}

    @Override
    public void exitDoWhileLoop(DoWhileLoopContext doWhileLoopContext) {}

    @Override
    public void enterWhileDoLoop(WhileDoLoopContext whileDoLoopContext) {}

    @Override
    public void exitWhileDoLoop(WhileDoLoopContext whileDoLoopContext) {}

    @Override
    public void enterWhileHead(WhileHeadContext whileHeadContext) {}

    @Override
    public void exitWhileHead(WhileHeadContext whileHeadContext) {}

    @Override
    public void enterDoStatement(DoStatementContext doStatementContext) {}

    @Override
    public void exitDoStatement(DoStatementContext doStatementContext) {}

    @Override
    public void enterDoBody(final DoBodyContext context) {
        pushScope(ScopeType.DO, Identifier.EMPTY);
    }

    @Override
    public void exitDoBody(final DoBodyContext context) {
        popScope();
    }

    @Override
    public void enterForLoop(final ForLoopContext context) {
        pushScope(ScopeType.FOR, Identifier.EMPTY);
    }

    @Override
    public void exitForLoop(final ForLoopContext context) {
        popScope();
    }

    @Override
    public void enterRangedLoopHead(RangedLoopHeadContext rangedLoopHeadContext) {}

    @Override
    public void exitRangedLoopHead(RangedLoopHeadContext rangedLoopHeadContext) {}

    @Override
    public void enterIndexedLoopHead(IndexedLoopHeadContext indexedLoopHeadContext) {}

    @Override
    public void exitIndexedLoopHead(IndexedLoopHeadContext indexedLoopHeadContext) {}

    @Override
    public void enterIfExpr(final IfExprContext context) {
        if(context.ifBody() != null) {
            pushScope(ScopeType.IF, Identifier.EMPTY);
        }
    }

    @Override
    public void exitIfExpr(final IfExprContext context) {
        if(context.ifBody() != null) {
            popScope();
        }
    }

    @Override
    public void enterElseIfExpr(final ElseIfExprContext context) {
        if(context.ifBody() != null) {
            pushScope(ScopeType.ELSE_IF, Identifier.EMPTY);
        }
    }

    @Override
    public void exitElseIfExpr(final ElseIfExprContext context) {
        if(context.ifBody() != null) {
            popScope();
        }
    }

    @Override
    public void enterElseExpr(final ElseExprContext context) {
        if(context.ifBody() != null) {
            pushScope(ScopeType.ELSE, Identifier.EMPTY);
        }
    }

    @Override
    public void exitElseExpr(final ElseExprContext context) {
        if(context.ifBody() != null) {
            popScope();
        }
    }

    @Override
    public void enterIfBody(IfBodyContext ifBodyContext) {}

    @Override
    public void exitIfBody(IfBodyContext ifBodyContext) {}

    @Override
    public void enterFunction(final FunctionContext context) {
        if(context.functionBody() != null) {
            pushScope(ScopeType.FUNCTION, FunctionUtils.getFunctionName(context.protoFunction().functionIdent()));
        }
    }

    @Override
    public void exitFunction(final FunctionContext context) {
        if(context.functionBody() != null) {
            popScope();
        }
    }

    @Override
    public void enterFunctionIdent(FunctionIdentContext functionIdentContext) {}

    @Override
    public void exitFunctionIdent(FunctionIdentContext functionIdentContext) {}

    @Override
    public void enterExternFunction(ExternFunctionContext externFunctionContext) {}

    @Override
    public void exitExternFunction(ExternFunctionContext externFunctionContext) {}

    @Override
    public void enterFunctionBody(FunctionBodyContext functionBodyContext) {}

    @Override
    public void exitFunctionBody(FunctionBodyContext functionBodyContext) {}

    @Override
    public void enterLetExpr(LetExprContext variableContext) {}

    @Override
    public void exitLetExpr(LetExprContext variableContext) {}

    @Override
    public void enterInlineFunctionBody(InlineFunctionBodyContext inlineFunctionBodyContext) {}

    @Override
    public void exitInlineFunctionBody(InlineFunctionBodyContext inlineFunctionBodyContext) {}

    @Override
    public void enterProtoFunction(ProtoFunctionContext protoFunctionContext) {}

    @Override
    public void exitProtoFunction(ProtoFunctionContext protoFunctionContext) {}

    @Override
    public void enterFunctionParamList(FunctionParamListContext functionParamListContext) {}

    @Override
    public void exitFunctionParamList(FunctionParamListContext functionParamListContext) {}

    @Override
    public void enterFunctionParam(FunctionParamContext functionParamContext) {}

    @Override
    public void exitFunctionParam(FunctionParamContext functionParamContext) {}

    @Override
    public void enterExprList(ExprListContext exprListContext) {}

    @Override
    public void exitExprList(ExprListContext exprListContext) {}

    @Override
    public void enterExpr(ExprContext exprContext) {}

    @Override
    public void exitExpr(ExprContext exprContext) {}

    @Override
    public void enterAssignmentExpr(AssignmentExprContext assignmentExprContext) {}

    @Override
    public void exitAssignmentExpr(AssignmentExprContext assignmentExprContext) {}

    @Override
    public void enterAlignofExpr(AlignofExprContext alignofExprContext) {}

    @Override
    public void exitAlignofExpr(AlignofExprContext alignofExprContext) {}

    @Override
    public void enterSizeofExpr(SizeofExprContext sizeofExprContext) {}

    @Override
    public void exitSizeofExpr(SizeofExprContext sizeofExprContext) {}

    @Override
    public void enterSpreadExpr(SpreadExprContext spreadExprContext) {}

    @Override
    public void exitSpreadExpr(SpreadExprContext spreadExprContext) {}

    @Override
    public void enterGroupedExpr(GroupedExprContext groupedExprContext) {}

    @Override
    public void exitGroupedExpr(GroupedExprContext groupedExprContext) {}

    @Override
    public void enterExhaustiveIfExpr(ExhaustiveIfExprContext exhaustiveIfExprContext) {}

    @Override
    public void exitExhaustiveIfExpr(ExhaustiveIfExprContext exhaustiveIfExprContext) {}

    @Override
    public void enterExhaustiveWhenExpr(ExhaustiveWhenExprContext exhaustiveWhenExprContext) {}

    @Override
    public void exitExhaustiveWhenExpr(ExhaustiveWhenExprContext exhaustiveWhenExprContext) {}

    @Override
    public void enterSizedSliceExpr(SizedSliceExprContext sizedArrayExprContext) {}

    @Override
    public void exitSizedSliceExpr(SizedSliceExprContext sizedArrayExprContext) {}

    @Override
    public void enterSliceInitExpr(SliceInitExprContext arrayInitExprContext) {}

    @Override
    public void exitSliceInitExpr(SliceInitExprContext arrayInitExprContext) {}

    @Override
    public void enterStackAllocExpr(StackAllocExprContext stackAllocExprContext) {}

    @Override
    public void exitStackAllocExpr(StackAllocExprContext stackAllocExprContext) {}

    @Override
    public void enterHeapInitExpr(HeapInitExprContext heapInitExprContext) {}

    @Override
    public void exitHeapInitExpr(HeapInitExprContext heapInitExprContext) {}

    @Override
    public void enterStackInitExpr(StackInitExprContext stackInitExprContext) {}

    @Override
    public void exitStackInitExpr(StackInitExprContext stackInitExprContext) {}

    @Override
    public void enterCallExpr(CallExprContext callExprContext) {}

    @Override
    public void exitCallExpr(CallExprContext callExprContext) {}

    @Override
    public void enterIncrementExpr(IncrementExprContext incrementExprContext) {}

    @Override
    public void exitIncrementExpr(IncrementExprContext incrementExprContext) {}

    @Override
    public void enterDecrementExpr(DecrementExprContext decrementExprContext) {}

    @Override
    public void exitDecrementExpr(DecrementExprContext decrementExprContext) {}

    @Override
    public void enterBinaryOp(BinaryOpContext binaryOpContext) {}

    @Override
    public void exitBinaryOp(BinaryOpContext binaryOpContext) {}

    @Override
    public void enterUnaryOp(UnaryOpContext unaryOpContext) {}

    @Override
    public void exitUnaryOp(UnaryOpContext unaryOpContext) {}

    @Override
    public void enterRef(RefContext refContext) {}

    @Override
    public void exitRef(RefContext refContext) {}

    @Override
    public void enterThisRef(ThisRefContext thisRefContext) {}

    @Override
    public void exitThisRef(ThisRefContext thisRefContext) {}

    @Override
    public void enterSimpleRef(SimpleRefContext simpleRefContext) {}

    @Override
    public void exitSimpleRef(SimpleRefContext simpleRefContext) {}

    @Override
    public void enterBinaryRefOp(BinaryRefOpContext binaryRefOpContext) {}

    @Override
    public void exitBinaryRefOp(BinaryRefOpContext binaryRefOpContext) {}

    @Override
    public void enterSpecialRef(SpecialRefContext specialRefContext) {}

    @Override
    public void exitSpecialRef(SpecialRefContext specialRefContext) {}

    @Override
    public void enterUnaryRefOp(UnaryRefOpContext unaryRefOpContext) {}

    @Override
    public void exitUnaryRefOp(UnaryRefOpContext unaryRefOpContext) {}

    @Override
    public void enterMethodRef(MethodRefContext methodRefContext) {}

    @Override
    public void exitMethodRef(MethodRefContext methodRefContext) {}

    @Override
    public void enterGenericParamList(GenericParamListContext genericParamListContext) {}

    @Override
    public void exitGenericParamList(GenericParamListContext genericParamListContext) {}

    @Override
    public void enterGenericParam(GenericParamContext genericParamContext) {}

    @Override
    public void exitGenericParam(GenericParamContext genericParamContext) {}

    @Override
    public void enterGenericExpr(GenericExprContext genericExprContext) {}

    @Override
    public void exitGenericExpr(GenericExprContext genericExprContext) {}

    @Override
    public void enterGenericGroupedExpr(GenericGroupedExprContext genericGroupedExprContext) {}

    @Override
    public void exitGenericGroupedExpr(GenericGroupedExprContext genericGroupedExprContext) {}

    @Override
    public void enterGenericOp(GenericOpContext genericOpContext) {}

    @Override
    public void exitGenericOp(GenericOpContext genericOpContext) {}

    @Override
    public void enterGenericList(GenericListContext genericListContext) {}

    @Override
    public void exitGenericList(GenericListContext genericListContext) {}

    @Override
    public void enterLiteral(LiteralContext literalContext) {}

    @Override
    public void exitLiteral(LiteralContext literalContext) {}

    @Override
    public void enterBoolLiteral(BoolLiteralContext boolLiteralContext) {}

    @Override
    public void exitBoolLiteral(BoolLiteralContext boolLiteralContext) {}

    @Override
    public void enterIntLiteral(IntLiteralContext intLiteralContext) {}

    @Override
    public void exitIntLiteral(IntLiteralContext intLiteralContext) {}

    @Override
    public void enterSintLiteral(SintLiteralContext sintLiteralContext) {}

    @Override
    public void exitSintLiteral(SintLiteralContext sintLiteralContext) {}

    @Override
    public void enterUintLiteral(UintLiteralContext uintLiteralContext) {}

    @Override
    public void exitUintLiteral(UintLiteralContext uintLiteralContext) {}

    @Override
    public void enterFloatLiteral(FloatLiteralContext floatLiteralContext) {}

    @Override
    public void exitFloatLiteral(FloatLiteralContext floatLiteralContext) {}

    @Override
    public void enterStringLiteral(StringLiteralContext stringLiteralContext) {}

    @Override
    public void exitStringLiteral(StringLiteralContext stringLiteralContext) {}

    @Override
    public void enterSimpleStringLiteral(SimpleStringLiteralContext simpleStringLiteralContext) {}

    @Override
    public void exitSimpleStringLiteral(SimpleStringLiteralContext simpleStringLiteralContext) {}

    @Override
    public void enterMultilineStringLiteral(MultilineStringLiteralContext multilineStringLiteralContext) {}

    @Override
    public void exitMultilineStringLiteral(MultilineStringLiteralContext multilineStringLiteralContext) {}

    @Override
    public void enterAccessMod(AccessModContext accessModContext) {}

    @Override
    public void exitAccessMod(AccessModContext accessModContext) {}

    @Override
    public void enterFunctionMod(FunctionModContext functionModContext) {}

    @Override
    public void exitFunctionMod(FunctionModContext functionModContext) {}

    @Override
    public void enterCallConvMod(CallConvModContext callConvModContext) {}

    @Override
    public void exitCallConvMod(CallConvModContext callConvModContext) {}

    @Override
    public void enterStorageMod(StorageModContext storageModContext) {}

    @Override
    public void exitStorageMod(StorageModContext storageModContext) {}

    @Override
    public void enterTypeMod(TypeModContext typeModContext) {}

    @Override
    public void exitTypeMod(TypeModContext typeModContext) {}

    @Override
    public void enterTypeList(TypeListContext typeListContext) {}

    @Override
    public void exitTypeList(TypeListContext typeListContext) {}

    @Override
    public void enterType(TypeContext typeContext) {}

    @Override
    public void exitType(TypeContext typeContext) {}

    @Override
    public void enterSimpleType(SimpleTypeContext simpleTypeContext) {}

    @Override
    public void exitSimpleType(SimpleTypeContext simpleTypeContext) {}

    @Override
    public void enterRefType(RefTypeContext refTypeContext) {}

    @Override
    public void exitRefType(RefTypeContext refTypeContext) {}

    @Override
    public void enterPointerType(PointerTypeContext pointerTypeContext) {}

    @Override
    public void exitPointerType(PointerTypeContext pointerTypeContext) {}

    @Override
    public void enterGenericType(GenericTypeContext genericTypeContext) {}

    @Override
    public void exitGenericType(GenericTypeContext genericTypeContext) {}

    @Override
    public void enterSliceType(SliceTypeContext arrayTypeContext) {}

    @Override
    public void exitSliceType(SliceTypeContext arrayTypeContext) {}

    @Override
    public void enterBuiltinType(BuiltinTypeContext builtinTypeContext) {}

    @Override
    public void exitBuiltinType(BuiltinTypeContext builtinTypeContext) {}

    @Override
    public void enterMiscType(MiscTypeContext miscTypeContext) {}

    @Override
    public void exitMiscType(MiscTypeContext miscTypeContext) {}

    @Override
    public void enterIntType(IntTypeContext intTypeContext) {}

    @Override
    public void exitIntType(IntTypeContext intTypeContext) {}

    @Override
    public void enterSintType(SintTypeContext sintTypeContext) {}

    @Override
    public void exitSintType(SintTypeContext sintTypeContext) {}

    @Override
    public void enterUintType(UintTypeContext uintTypeContext) {}

    @Override
    public void exitUintType(UintTypeContext uintTypeContext) {}

    @Override
    public void enterFloatType(FloatTypeContext floatTypeContext) {}

    @Override
    public void exitFloatType(FloatTypeContext floatTypeContext) {}

    @Override
    public void enterQualifiedIdent(QualifiedIdentContext qualifiedIdentContext) {}

    @Override
    public void exitQualifiedIdent(QualifiedIdentContext qualifiedIdentContext) {}

    @Override
    public void enterLerpIdent(LerpIdentContext lerpIdentContext) {}

    @Override
    public void exitLerpIdent(LerpIdentContext lerpIdentContext) {}

    @Override
    public void enterIdent(IdentContext identContext) {}

    @Override
    public void exitIdent(IdentContext identContext) {}

    @Override
    public void enterSpecialToken(SpecialTokenContext specialTokenContext) {}

    @Override
    public void exitSpecialToken(SpecialTokenContext specialTokenContext) {}

    @Override
    public void enterEnd(EndContext endContext) {}

    @Override
    public void exitEnd(EndContext endContext) {}

    @Override
    public void visitTerminal(TerminalNode terminalNode) {}

    @Override
    public void visitErrorNode(ErrorNode errorNode) {}

    @Override
    public void enterEveryRule(ParserRuleContext parserRuleContext) {}

    @Override
    public void exitEveryRule(ParserRuleContext parserRuleContext) {}
    // @formatter:on
}

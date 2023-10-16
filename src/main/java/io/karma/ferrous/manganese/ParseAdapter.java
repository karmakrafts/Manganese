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

import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.scope.ScopeStack;
import io.karma.ferrous.manganese.scope.ScopeType;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.ferrous.vanadium.FerrousParserListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
public abstract class ParseAdapter implements FerrousParserListener {
    protected ScopeStack scopeStack = new ScopeStack();
    protected Compiler compiler;

    protected ParseAdapter(final Compiler compiler) {
        this.compiler = compiler;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public ScopeStack getScopeStack() {
        return scopeStack;
    }

    // @formatter:off
    @Override
    public void enterFile(FileContext fileContext) {
        scopeStack.push(Scope.GLOBAL);
    }

    @Override
    public void exitFile(FileContext fileContext) {
        scopeStack.pop();
    }

    @Override
    public void enterModuleFile(ModuleFileContext moduleFileContext) {
        scopeStack.push(Scope.GLOBAL);
    }

    @Override
    public void exitModuleFile(ModuleFileContext moduleFileContext) {
        scopeStack.pop();
    }

    @Override
    public void enterModule(ModuleContext moduleContext) {}

    @Override
    public void exitModule(ModuleContext moduleContext) {}

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
        scopeStack.push(new Scope(ScopeType.ENUM_CLASS, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitEnumClass(EnumClassContext context) {
        scopeStack.pop();
    }

    @Override
    public void enterClass(ClassContext context) {
        scopeStack.push(new Scope(ScopeType.CLASS, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitClass(ClassContext context) {
        scopeStack.pop();
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
        scopeStack.push(new Scope(ScopeType.ENUM, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitEnum(EnumContext enumContext) {
        scopeStack.pop();
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
        scopeStack.push(new Scope(ScopeType.STRUCT, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitStruct(StructContext context) {
        scopeStack.pop();
    }

    @Override
    public void enterInterfaceBody(InterfaceBodyContext interfaceBodyContext) {}

    @Override
    public void exitInterfaceBody(InterfaceBodyContext interfaceBodyContext) {}

    @Override
    public void enterInterface(InterfaceContext context) {
        scopeStack.push(new Scope(ScopeType.INTERFACE, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitInterface(InterfaceContext context) {
        scopeStack.pop();
    }

    @Override
    public void enterAttribBody(AttribBodyContext attribBodyContext) {}

    @Override
    public void exitAttribBody(AttribBodyContext attribBodyContext) {}

    @Override
    public void enterAttrib(AttribContext context) {
        scopeStack.push(new Scope(ScopeType.ATTRIBUTE, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitAttrib(AttribContext context) {
        scopeStack.pop();
    }

    @Override
    public void enterTrait(TraitContext context) {
        scopeStack.push(new Scope(ScopeType.TRAIT, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitTrait(TraitContext context) {
        scopeStack.pop();
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
    public void enterConstructor(ConstructorContext constructorContext) {}

    @Override
    public void exitConstructor(ConstructorContext constructorContext) {}

    @Override
    public void enterThisCall(ThisCallContext thisCallContext) {}

    @Override
    public void exitThisCall(ThisCallContext thisCallContext) {}

    @Override
    public void enterSuperCall(SuperCallContext superCallContext) {}

    @Override
    public void exitSuperCall(SuperCallContext superCallContext) {}

    @Override
    public void enterDestructor(DestructorContext destructorContext) {}

    @Override
    public void exitDestructor(DestructorContext destructorContext) {}

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
    public void enterWhenStatement(WhenStatementContext whenStatementContext) {}

    @Override
    public void exitWhenStatement(WhenStatementContext whenStatementContext) {}

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
    public void enterLoop(LoopContext loopContext) {}

    @Override
    public void exitLoop(LoopContext loopContext) {}

    @Override
    public void enterWhileLoop(WhileLoopContext whileLoopContext) {}

    @Override
    public void exitWhileLoop(WhileLoopContext whileLoopContext) {}

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
    public void enterDoBlock(DoBlockContext doBlockContext) {}

    @Override
    public void exitDoBlock(DoBlockContext doBlockContext) {}

    @Override
    public void enterForLoop(ForLoopContext forLoopContext) {}

    @Override
    public void exitForLoop(ForLoopContext forLoopContext) {}

    @Override
    public void enterRangedLoopHead(RangedLoopHeadContext rangedLoopHeadContext) {}

    @Override
    public void exitRangedLoopHead(RangedLoopHeadContext rangedLoopHeadContext) {}

    @Override
    public void enterIndexedLoopHead(IndexedLoopHeadContext indexedLoopHeadContext) {}

    @Override
    public void exitIndexedLoopHead(IndexedLoopHeadContext indexedLoopHeadContext) {}

    @Override
    public void enterIfStatement(IfStatementContext ifStatementContext) {}

    @Override
    public void exitIfStatement(IfStatementContext ifStatementContext) {}

    @Override
    public void enterElseIfStatement(ElseIfStatementContext elseIfStatementContext) {}

    @Override
    public void exitElseIfStatement(ElseIfStatementContext elseIfStatementContext) {}

    @Override
    public void enterElseStatement(ElseStatementContext elseStatementContext) {}

    @Override
    public void exitElseStatement(ElseStatementContext elseStatementContext) {}

    @Override
    public void enterIfBody(IfBodyContext ifBodyContext) {}

    @Override
    public void exitIfBody(IfBodyContext ifBodyContext) {}

    @Override
    public void enterFunction(FunctionContext functionContext) {}

    @Override
    public void exitFunction(FunctionContext functionContext) {}

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
    public void enterVariable(VariableContext variableContext) {}

    @Override
    public void exitVariable(VariableContext variableContext) {}

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
    public void enterFunctionParamType(FunctionParamTypeContext functionParamTypeContext) {}

    @Override
    public void exitFunctionParamType(FunctionParamTypeContext functionParamTypeContext) {}

    @Override
    public void enterExprList(ExprListContext exprListContext) {}

    @Override
    public void exitExprList(ExprListContext exprListContext) {}

    @Override
    public void enterExpr(ExprContext exprContext) {}

    @Override
    public void exitExpr(ExprContext exprContext) {}

    @Override
    public void enterReAssignmentExpr(ReAssignmentExprContext reAssignmentExprContext) {}

    @Override
    public void exitReAssignmentExpr(ReAssignmentExprContext reAssignmentExprContext) {}

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
    public void enterSimpleExpr(SimpleExprContext simpleExprContext) {}

    @Override
    public void exitSimpleExpr(SimpleExprContext simpleExprContext) {}

    @Override
    public void enterBinaryExpr(BinaryExprContext binaryExprContext) {}

    @Override
    public void exitBinaryExpr(BinaryExprContext binaryExprContext) {}

    @Override
    public void enterBinaryOp(BinaryOpContext binaryOpContext) {}

    @Override
    public void exitBinaryOp(BinaryOpContext binaryOpContext) {}

    @Override
    public void enterUnaryExpr(UnaryExprContext unaryExprContext) {}

    @Override
    public void exitUnaryExpr(UnaryExprContext unaryExprContext) {}

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
    public void enterGenericBinaryExpr(GenericBinaryExprContext genericBinaryExprContext) {}

    @Override
    public void exitGenericBinaryExpr(GenericBinaryExprContext genericBinaryExprContext) {}

    @Override
    public void enterGenericBinaryExprLhs(GenericBinaryExprLhsContext genericBinaryExprLhsContext) {}

    @Override
    public void exitGenericBinaryExprLhs(GenericBinaryExprLhsContext genericBinaryExprLhsContext) {}

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
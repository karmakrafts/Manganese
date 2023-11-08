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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.access.AccessKind;
import io.karma.ferrous.manganese.ocm.access.ScopedAccess;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.parser.FieldLayoutParser;
import io.karma.ferrous.manganese.parser.ProtoFunctionParser;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.*;
import io.karma.ferrous.vanadium.FerrousParser.*;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class PreAnalyzer extends ParseAdapter {
    private final LinkedHashMap<Identifier, NamedType> udts = new LinkedHashMap<>();
    private final HashMap<Identifier, HashMap<FunctionType, Function>> functions = new HashMap<>();

    public PreAnalyzer(final Compiler compiler, final CompileContext compileContext) {
        super(compiler, compileContext);
    }

    private boolean addTypesToGraph(final ArrayDeque<Pair<TopoNode<NamedType>, TopoNode<NamedType>>> typesToResolve,
                                    final Map<Identifier, TopoNode<NamedType>> nodes,
                                    final HashSet<Identifier> resolved) {
        final var buffer = Ansi.ansi().fgBright(Color.CYAN);
        final var pair = typesToResolve.pop();

        final var parentNode = pair.getLeft();
        final var childNode = pair.getRight();
        final var childType = childNode.getValue();

        if (childType instanceof UDT udt && !udt.isComplete()) {
            buffer.fgBright(Color.MAGENTA).a('U').fgBright(Color.CYAN);
            final var fields = udt.fields();
            for (final var field : fields) {
                final var fieldType = field.getType();
                if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType namedType)) {
                    continue; // Skip all types which are complete to save time
                }
                var fieldTypeName = namedType.getQualifiedName();
                final var fieldNode = ScopeUtils.findInScope(nodes, fieldTypeName, childType.getScopeName());
                if (fieldNode == null) {
                    final var token = field.getTokenSlice().findTokenOrFirst(fieldTypeName.toString());
                    compileContext.reportError(token, CompileErrorCode.E3004);
                    return false;
                }
                fieldTypeName = fieldNode.getValue().getQualifiedName();
                if (resolved.contains(fieldTypeName)) {
                    buffer.fgBright(Color.GREEN).a('C').fgBright(Color.CYAN);
                    continue; // Prevent multiple passes over the same type
                }
                typesToResolve.push(Pair.of(childNode, fieldNode));
                resolved.add(fieldTypeName);
                buffer.a('R');
            }
        }

        if (childType instanceof AliasedType alias && !alias.isComplete()) {
            buffer.fgBright(Color.MAGENTA).a('A').fgBright(Color.CYAN);
            if (!alias.isBuiltin() && !alias.isComplete()) {
                final var backingType = alias.getBackingType();
                if (!(backingType instanceof NamedType namedType)) {
                    // TODO: report error?
                    return false;
                }
                var backingTypeName = namedType.getQualifiedName();
                final var typeNode = ScopeUtils.findInScope(nodes, backingTypeName, childType.getScopeName());
                if (typeNode == null) {
                    compileContext.reportError(backingTypeName.toString(), CompileErrorCode.E3004);
                    return false;
                }
                backingTypeName = typeNode.getValue().getQualifiedName();
                if (!resolved.contains(backingTypeName)) { // Prevent multiple passes over the same type
                    typesToResolve.push(Pair.of(childNode, typeNode));
                    resolved.add(backingTypeName);
                    buffer.a('R');
                }
                else {
                    buffer.fgBright(Color.GREEN).a('C').fgBright(Color.CYAN);
                }
            }
        }

        parentNode.addDependency(childNode);
        buffer.a('D').fg(Color.CYAN).a(" > ").a(childType);
        Logger.INSTANCE.debugln("Resolved type graph: %s", buffer.toString());

        return true;
    }

    @Override
    public void enterFunction(final FunctionContext context) {
        if (checkIsFunctionAlreadyDefined(context.protoFunction())) {
            return;
        }
        final var analyzer = new ProtoFunctionParser(compiler,
            compileContext,
            scopeStack,
            false,
            TokenSlice.from(compileContext, context));
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        final var function = analyzer.getFunction();
        functions.computeIfAbsent(function.getQualifiedName(), n -> new HashMap<>()).put(function.getType(), function);
        super.enterFunction(context);
    }

    @Override
    public void enterExternFunction(final ExternFunctionContext context) {
        if (checkIsFunctionAlreadyDefined(context.protoFunction())) {
            return;
        }
        final var analyzer = new ProtoFunctionParser(compiler,
            compileContext,
            scopeStack,
            true,
            TokenSlice.from(compileContext, context));
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        final var function = analyzer.getFunction();
        functions.computeIfAbsent(function.getQualifiedName(), n -> new HashMap<>()).put(function.getType(), function);
        super.enterExternFunction(context);
    }

    @Override
    public void enterTypeAlias(final TypeAliasContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        final var type = TypeUtils.getType(compiler, compileContext, scopeStack, context.type());
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        final var aliasedType = Types.aliased(Utils.getIdentifier(identContext),
            type,
            scopeStack::applyEnclosingScopes,
            TokenSlice.from(compileContext, context),
            genericParams);
        udts.put(aliasedType.getQualifiedName(), aliasedType);
        super.enterTypeAlias(context);
    }

    @Override
    public void enterAttrib(AttribContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), genericParams, UDTKind.ATTRIBUTE);
        super.enterAttrib(context);
    }

    @Override
    public void enterStruct(final StructContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), genericParams, UDTKind.STRUCT);
        super.enterStruct(context);
    }

    @Override
    public void enterClass(final ClassContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), genericParams, UDTKind.CLASS);
        super.enterClass(context);
    }

    @Override
    public void enterEnumClass(final EnumClassContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), new GenericParameter[0], UDTKind.ENUM_CLASS);
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(final TraitContext context) {
        final var identContext = context.ident();
        if (checkIsTypeAlreadyDefined(identContext)) {
            return;
        }
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), genericParams, UDTKind.TRAIT);
        super.enterTrait(context);
    }

    private boolean checkIsFunctionAlreadyDefined(final ProtoFunctionContext context) {
        final var identContext = context.functionIdent();
        final var name = FunctionUtils.getFunctionName(identContext);
        final var overloadSet = functions.get(name);
        if (overloadSet != null) {
            final var type = FunctionUtils.getFunctionType(compiler, compileContext, scopeStack, context);
            final var function = overloadSet.get(type);
            if(function != null) {
                final var message = Utils.makeCompilerMessage(String.format("Function '%s' is already defined", name));
                compileContext.reportError(identContext.start, message, CompileErrorCode.E4003);
                return true;
            }
        }
        return false;
    }

    private boolean checkIsTypeAlreadyDefined(final IdentContext identContext) {
        final var name = Utils.getIdentifier(identContext);
        final var type = udts.get(name);
        if (type != null) {
            final var message = Utils.makeCompilerMessage(String.format("Type '%s' is already defined", name));
            compileContext.reportError(identContext.start, message, CompileErrorCode.E3000);
            return true;
        }
        return false;
    }

    public @Nullable Type findCompleteTypeInScope(final Identifier name, final Identifier scopeName) {
        Profiler.INSTANCE.push();
        Type type = ScopeUtils.findInScope(udts, name, scopeName);
        if (type == null) {
            return null;
        }
        while (type instanceof AliasedType alias) {
            type = alias.getBackingType();
        }
        if (!type.isComplete()) {
            if (!(type instanceof NamedType namedType)) {
                return null;
            }
            final var typeName = namedType.getQualifiedName();
            type = ScopeUtils.findInScope(udts, typeName, scopeName);
        }
        Profiler.INSTANCE.pop();
        return type;
    }

    public @Nullable Type findCompleteType(final NamedType type) {
        if (type.isComplete()) {
            return type;
        }
        return findCompleteTypeInScope(type.getName(), type.getScopeName());
    }

    public @Nullable Type findTypeInScope(final Identifier name, final Identifier scopeName) {
        try {
            Profiler.INSTANCE.push();
            return ScopeUtils.findInScope(udts, name, scopeName);
        }
        finally {
            Profiler.INSTANCE.pop();
        }
    }

    private boolean resolveAliasedType(final AliasedType alias, final Identifier scopeName) {
        Profiler.INSTANCE.push();
        var currentType = alias.getBackingType();
        while (!currentType.isComplete()) {
            if (currentType instanceof AliasedType aliasedType) {
                currentType = aliasedType.getBackingType();
                continue;
            }
            if (!(currentType instanceof NamedType namedType)) {
                return false;
            }
            final var currentTypeName = namedType.getQualifiedName();
            final var completeType = findCompleteTypeInScope(currentTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            currentType = completeType;
        }
        alias.setBackingType(currentType);
        Profiler.INSTANCE.pop();
        return true;
    }

    private boolean resolveFieldTypes(final UDT udt, final Identifier scopeName) {
        Profiler.INSTANCE.push();
        final var type = udt.type();
        final var fieldTypes = type.getFieldTypes();
        final var numFields = fieldTypes.size();
        for (var i = 0; i < numFields; i++) {
            final var fieldType = fieldTypes.get(i);
            if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType namedType)) {
                continue;
            }
            final var fieldTypeName = namedType.getQualifiedName();
            Logger.INSTANCE.debugln("Found incomplete field type '%s' in '%s'", fieldTypeName, scopeName);
            final var completeType = findCompleteTypeInScope(fieldTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            Logger.INSTANCE.debugln("  > Resolved to complete type '%s'", completeType);
            type.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
        Profiler.INSTANCE.pop();
        return true;
    }

    private void resolveUDTs() {
        Profiler.INSTANCE.push();
        final var udts = this.udts.values();
        for (final var udt : udts) {
            final var scopeName = udt.getScopeName();
            if (udt.isAliased() && udt instanceof AliasedType alias) {
                if (!resolveAliasedType(alias, scopeName)) {
                    compileContext.reportError(alias.getTokenSlice().getFirstToken(), CompileErrorCode.E3003);
                }
            }
            if (!(udt instanceof UDT actualUdt)) {
                continue; // Skip everything else apart from UDTs
            }
            if (!resolveFieldTypes(actualUdt, scopeName)) {
                compileContext.reportError(actualUdt.tokenSlice().getFirstToken(), CompileErrorCode.E3004);
            }
        }
        Profiler.INSTANCE.pop();
    }

    private void materializeTypes() {
        Profiler.INSTANCE.push();
        for (final var udt : udts.values()) {
            if (udt.isAliased()) {
                continue; // Don't need to waste time on doing nothing..
            }
            if (!udt.isComplete()) {
                Logger.INSTANCE.errorln("Cannot materialize type '%s' as it is incomplete", udt.getQualifiedName());
                continue;
            }
            final var address = udt.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materialized type %s at 0x%08X", udt.getQualifiedName(), address);
        }
        Profiler.INSTANCE.pop();
    }

    private void sortTypes() {
        Profiler.INSTANCE.push();
        final var rootNode = new TopoNode<NamedType>(DummyType.INSTANCE);
        // @formatter:off
        final var nodes = udts.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        final var queue = new ArrayDeque<Pair<TopoNode<NamedType>, TopoNode<NamedType>>>(); // <parent_node, child_node>
        final var resolved = new HashSet<Identifier>();

        outer:
        for (final var nodeEntry : nodes.entrySet()) {
            final var node = nodeEntry.getValue();
            final var type = node.getValue();
            if (type == null) {
                continue; // Skip sorting
            }
            rootNode.getValue().setEnclosingScope(type.getEnclosingScope());
            queue.push(Pair.of(rootNode, node));
            while (!queue.isEmpty()) {
                if (!addTypesToGraph(queue, nodes, resolved)) {
                    break outer;
                }
            }
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d type entries", udts.size());
        final var sortedNodes = new TopoSorter<>(rootNode).sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, NamedType>();

        for (final var node : sortedNodes) {
            final var type = node.getValue();
            if (type == null || type == NullType.INSTANCE || type == DummyType.INSTANCE) {
                continue;
            }
            final var qualifiedName = type.getQualifiedName();
            sortedMap.put(qualifiedName, udts.get(qualifiedName));
        }

        udts.clear();
        udts.putAll(sortedMap);
        Profiler.INSTANCE.pop();
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final Identifier name,
                                    final GenericParameter[] genericParams, final UDTKind kind) {
        final var layoutAnalyzer = new FieldLayoutParser(compiler, compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

        final var fields = layoutAnalyzer.getFields();
        final var fieldTypes = fields.stream().map(Field::getType).toArray(Type[]::new);
        final var tokenSlice = TokenSlice.from(compileContext, parent);
        final var type = Types.structure(name, scopeStack::applyEnclosingScopes, genericParams, tokenSlice, fieldTypes);
        final var udt = new UDT(kind, type, fields, tokenSlice);
        udts.put(type.getQualifiedName(), udt);

        Logger.INSTANCE.debugln("Captured field layout for type '%s'", type.getQualifiedName());
    }

    private void resolveTypeAccess() {
        Profiler.INSTANCE.push();
        final var udts = this.udts.values();
        for (final var udt : udts) {
            if (!(udt instanceof UDT actualUdt)) {
                continue; // Skip any non-UDTs
            }
            final var fields = actualUdt.fields();
            for (final var field : fields) {
                final var access = field.getAccess();
                if (access.getKind() != AccessKind.SCOPED) {
                    continue;
                }
                final var scopedAccess = (ScopedAccess) access;
                final var types = scopedAccess.types();
                final var numTypes = types.length;
                for (var i = 0; i < numTypes; i++) {
                    var type = types[i];
                    if (type.isComplete() || !(type instanceof NamedType namedType)) {
                        continue;
                    }
                    final var completeType = findCompleteType(namedType);
                    if (completeType == null) {
                        final var token = scopedAccess.tokenSlice().findTokenOrFirst(namedType.getQualifiedName().toString());
                        compileContext.reportError(token, CompileErrorCode.E3002);
                        continue;
                    }
                    types[i] = completeType;
                    Logger.INSTANCE.debugln("Resolved scoped access type '%s'", completeType);
                }
            }
        }
        Profiler.INSTANCE.pop();
    }

    private void checkTypeAccess() {
        // TODO: implement access checks
    }

    private void resolveFunctionTypes() {
        Profiler.INSTANCE.push();
        final var overloadSets = this.functions.values();
        for (final var overloadSet : overloadSets) {
            for (final var function : overloadSet.values()) {
                final var params = function.getParameters();
                for (final var param : params) {
                    final var type = param.getType();
                    if (!(type instanceof NamedType namedType)) {
                        continue;
                    }
                    final var completeType = findCompleteType(namedType);
                    if (completeType == null) {
                        compileContext.reportError(type.getTokenSlice().getFirstToken(), CompileErrorCode.E3005);
                        continue;
                    }
                    param.setType(completeType);
                }
            }
        }
        Profiler.INSTANCE.pop();
    }

    public void preProcess() {
        sortTypes();
        resolveUDTs();
        materializeTypes();
        resolveTypeAccess();
        checkTypeAccess();
        resolveFunctionTypes();
    }

    public void preMaterializeFunctionPrototypes(final Module module) {
        Logger.INSTANCE.debugln("Pre-materializing function prototypes");
        Profiler.INSTANCE.push();
        final var overloadSets = functions.values();
        for (final var overloadSet : overloadSets) {
            final var functions = overloadSet.values();
            for (final var function : functions) {
                function.materializePrototype(module, compiler.getTargetMachine());
            }
        }
        Profiler.INSTANCE.pop();
    }

    public HashMap<Identifier, HashMap<FunctionType, Function>> getFunctions() {
        return functions;
    }

    // TODO: implement matching against C-variadic functions
    public @Nullable Function findFunctionInScope(final Identifier name, final Identifier scopeName,
                                                  final @Nullable Type returnType, final Type... paramTypes) {
        try {
            Profiler.INSTANCE.push();
            final var overloadSet = ScopeUtils.findInScope(functions, name, scopeName);
            if (overloadSet == null) {
                return null;
            }
            final var types = overloadSet.keySet();
            for (final var type : types) {
                if ((returnType != null && !type.getReturnType().equals(returnType)) || !Arrays.equals(type.getParamTypes(),
                    paramTypes)) {
                    continue;
                }
                return overloadSet.get(type);
            }
            return null;
        }
        finally {
            Profiler.INSTANCE.pop();
        }
    }

    public @Nullable Function findFunctionInScope(final Identifier name, final Identifier scopeName,
                                                  final FunctionType type) {
        return findFunctionInScope(name, scopeName, type.getReturnType(), type.getParamTypes());
    }

    private static final class DummyType implements NamedType {
        public static final DummyType INSTANCE = new DummyType();
        private Scope enclosingScope;

        // @formatter:off
        private DummyType() {}
        // @formatter:on

        @Override
        public TokenSlice getTokenSlice() {
            return TokenSlice.EMPTY;
        }

        @Override
        public GenericParameter[] getGenericParams() {
            return new GenericParameter[0];
        }

        @Override
        public Identifier getName() {
            return Identifier.EMPTY;
        }

        @Override
        public long materialize(final TargetMachine machine) {
            throw new UnsupportedOperationException("Dummy type cannot be materialized");
        }

        @Override
        public TypeAttribute[] getAttributes() {
            return new TypeAttribute[0];
        }

        @Override
        public Type getBaseType() {
            return this;
        }

        @Override
        public @Nullable Scope getEnclosingScope() {
            return enclosingScope;
        }

        @Override
        public void setEnclosingScope(final Scope enclosingScope) {
            this.enclosingScope = enclosingScope;
        }
    }
}

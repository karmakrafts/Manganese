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

package io.karma.ferrous.manganese.analyze;

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.access.AccessKind;
import io.karma.ferrous.manganese.ocm.access.ScopedAccess;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.type.*;
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
public final class Analyzer extends ParseAdapter {
    private final LinkedHashMap<Identifier, NamedType> udts = new LinkedHashMap<>();
    private final HashMap<Identifier, Function> functions = new HashMap<>();

    public Analyzer(final Compiler compiler, final CompileContext compileContext) {
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
            final var fieldTypes = udt.type().getFieldTypes();
            for (final var fieldType : fieldTypes) {
                if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType)) {
                    continue; // Skip all types which are complete to save time
                }
                var fieldTypeName = ((NamedType) fieldType).getQualifiedName();
                final var fieldNode = ScopeUtils.findInScope(nodes, fieldTypeName, childType.getScopeName());
                if (fieldNode == null) {
                    compileContext.reportError(compileContext.makeError(fieldTypeName.toString(),
                        CompileErrorCode.E3004));
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
                var backingTypeName = ((NamedType) alias.getBackingType()).getQualifiedName();
                final var typeNode = ScopeUtils.findInScope(nodes, backingTypeName, childType.getScopeName());
                if (typeNode == null) {
                    compileContext.reportError(compileContext.makeError(backingTypeName.toString(),
                        CompileErrorCode.E3004));
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
        final var analyzer = new FunctionAnalyzer(compiler, compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(analyzer, context);
        final var function = analyzer.getFunction();
        super.enterFunction(context);
    }

    @Override
    public void enterTypeAlias(final TypeAliasContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        final var type = Objects.requireNonNull(TypeUtils.getType(compiler,
            compileContext,
            scopeStack,
            context.type()));
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        final var aliasedType = Types.aliased(Utils.getIdentifier(identContext),
            type,
            scopeStack::applyEnclosingScopes,
            genericParams);
        udts.put(aliasedType.getQualifiedName(), aliasedType);
        super.enterTypeAlias(context);
    }

    @Override
    public void enterAttrib(AttribContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
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
        if (checkIsAlreadyDefined(identContext)) {
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
        if (checkIsAlreadyDefined(identContext)) {
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
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), new GenericParameter[0], UDTKind.ENUM_CLASS);
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(final TraitContext context) {
        final var identContext = context.ident();
        if (checkIsAlreadyDefined(identContext)) {
            return;
        }
        final var genericParams = TypeUtils.getGenericParams(compiler,
            compileContext,
            scopeStack,
            context.genericParamList()).toArray(GenericParameter[]::new);
        analyzeFieldLayout(context, Utils.getIdentifier(identContext), genericParams, UDTKind.TRAIT);
        super.enterTrait(context);
    }

    private boolean checkIsAlreadyDefined(final IdentContext identContext) {
        final var name = Utils.getIdentifier(identContext);
        final var type = findTypeInScope(name, scopeStack.getScopeName());
        if (type != null) {
            final var message = Utils.makeCompilerMessage(String.format("Type '%s' is already defined", name));
            compileContext.reportError(compileContext.makeError(identContext.start, message, CompileErrorCode.E3000));
            return true;
        }
        return false;
    }

    public @Nullable Type findCompleteTypeInScope(final Identifier name, final Identifier scopeName) {
        Type type = ScopeUtils.findInScope(udts, name, scopeName);
        if (type == null) {
            return null;
        }
        while (type.isAliased()) {
            type = ((AliasedType) type).getBackingType();
        }
        if (!type.isComplete()) {
            if (!(type instanceof NamedType)) {
                return null;
            }
            final var typeName = ((NamedType) type).getQualifiedName();
            type = ScopeUtils.findInScope(udts, typeName, scopeName);
        }
        return type;
    }

    public @Nullable Type findCompleteType(final NamedType type) {
        return findCompleteTypeInScope(type.getName(), type.getScopeName());
    }

    public @Nullable Type findTypeInScope(final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(udts, name, scopeName);
    }

    private boolean resolveAliasedType(final AliasedType alias, final Identifier scopeName) {
        var currentType = alias.getBackingType();
        while (!currentType.isComplete()) {
            if (currentType.isAliased()) {
                currentType = ((AliasedType) currentType).getBackingType();
                continue;
            }
            if (!(currentType instanceof NamedType)) {
                return false;
            }
            final var currentTypeName = ((NamedType) currentType).getQualifiedName();
            final var completeType = findCompleteTypeInScope(currentTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            currentType = completeType;
        }
        alias.setBackingType(currentType);
        return true;
    }

    private boolean resolveFieldTypes(final UDT udt, final Identifier scopeName) {
        final var type = udt.type();
        final var fieldTypes = type.getFieldTypes();
        final var numFields = fieldTypes.size();
        for (var i = 0; i < numFields; i++) {
            final var fieldType = fieldTypes.get(i);
            if (fieldType.isBuiltin() || fieldType.isComplete() || !(fieldType instanceof NamedType)) {
                continue;
            }
            final var fieldTypeName = ((NamedType) fieldType).getQualifiedName();
            Logger.INSTANCE.debugln("Found incomplete field type '%s' in '%s'", fieldTypeName, scopeName);
            final var completeType = findCompleteTypeInScope(fieldTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            Logger.INSTANCE.debugln("  > Resolved to complete type '%s'", completeType);
            type.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
        return true;
    }

    private void resolveTypes() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            final var scopeName = udt.getScopeName();
            if (udt.isAliased() && udt instanceof AliasedType alias) {
                if (!resolveAliasedType(alias, scopeName)) {
                    compileContext.reportError(compileContext.makeError(alias.toString(), CompileErrorCode.E3003));
                }
            }
            if (!(udt instanceof UDT)) {
                continue; // Skip everything else apart from UDTs
            }
            if (!resolveFieldTypes((UDT) udt, scopeName)) {
                compileContext.reportError(compileContext.makeError(udt.toString(), CompileErrorCode.E3004));
            }
        }
    }

    private void materializeTypes() {
        for (final var udt : udts.values()) {
            if (udt.isAliased()) {
                continue; // Don't need to waste time on doing nothing..
            }
            if (!udt.isComplete()) {
                Logger.INSTANCE.errorln("Cannot materialize type '%s' as it is incomplete", udt.getQualifiedName());
                continue;
            }
            udt.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materializing type %s", udt.getQualifiedName());
        }
    }

    private void sortTypes() {
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
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final Identifier name,
                                    final GenericParameter[] genericParams, final UDTKind kind) {
        final var layoutAnalyzer = new FieldLayoutAnalyzer(compiler, compileContext, scopeStack);
        ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

        final var fields = layoutAnalyzer.getFields();
        final var fieldTypes = fields.stream().map(Field::getType).toArray(Type[]::new);
        final var type = Types.structure(name, scopeStack::applyEnclosingScopes, genericParams, fieldTypes);
        final var udt = new UDT(kind, type, fields);
        udts.put(type.getQualifiedName(), udt);

        Logger.INSTANCE.debugln("Captured field layout for type '%s'", type.getQualifiedName());
    }

    private void resolveTypeAccess() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            if (!(udt instanceof UDT)) {
                continue; // Skip any non-UDTs
            }
            final var fields = ((UDT) udt).fields();
            for (final var field : fields) {
                final var access = field.getAccess();
                if (access.getKind() != AccessKind.SCOPED) {
                    continue;
                }
                final var types = ((ScopedAccess) access).types();
                final var numTypes = types.length;
                for (var i = 0; i < numTypes; i++) {
                    var type = types[i];
                    if (type.isComplete() || !(type instanceof NamedType)) {
                        continue;
                    }
                    final var completeType = findCompleteType((NamedType) type);
                    if (completeType == null) {
                        compileContext.reportError(compileContext.makeError(CompileErrorCode.E3002));
                        continue;
                    }
                    types[i] = completeType;
                    Logger.INSTANCE.debugln("Resolved scoped access type '%s'", completeType);
                }
            }
        }
    }

    private void checkTypeAccess() {
        // TODO: implement access checks
    }

    public void preProcessTypes() {
        sortTypes();
        resolveTypes();
        materializeTypes();
        resolveTypeAccess();
        checkTypeAccess();
    }

    private static final class DummyType implements NamedType {
        public static final DummyType INSTANCE = new DummyType();
        private Scope enclosingScope;

        // @formatter:off
        private DummyType() {}
        // @formatter:on

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

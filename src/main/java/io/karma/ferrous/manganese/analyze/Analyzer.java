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
import io.karma.ferrous.manganese.compiler.CompileError;
import io.karma.ferrous.manganese.compiler.CompileStatus;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.access.DefaultAccess;
import io.karma.ferrous.manganese.ocm.type.AliasedType;
import io.karma.ferrous.manganese.ocm.type.StructureType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.UDT;
import io.karma.ferrous.manganese.ocm.type.UDTKind;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;
import io.karma.ferrous.vanadium.FerrousParser.TraitContext;
import io.karma.ferrous.vanadium.FerrousParser.TypeAliasContext;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class Analyzer extends ParseAdapter {
    private final LinkedHashMap<Identifier, Type> udts = new LinkedHashMap<>();

    public Analyzer(Compiler compiler) {
        super(compiler);
    }

    private static <T extends Type> void addTypesToGraph(final ArrayDeque<Type> typesToResolve, final TopoNode<T> node,
                                                         final Map<Identifier, TopoNode<T>> nodes) {
        while (!typesToResolve.isEmpty()) {
            final var type = typesToResolve.pop();
            if (type instanceof StructureType struct) {
                final var fieldTypes = struct.getFieldTypes();
                for (final var fieldType : fieldTypes) {
                    if (fieldType.isBuiltin() || fieldType.isComplete()) {
                        continue; // Skip all types which are complete to save time
                    }
                    typesToResolve.push(fieldType);
                }
                return;
            }
            var typeNode = nodes.get(type.getQualifiedName());
            if (typeNode == null) {
                final var enclosingName = node.getValue().getQualifiedName();
                typeNode = nodes.get(enclosingName.join(type.getQualifiedName()));
                if (typeNode == null) {
                    return;
                }
            }
            node.addDependency(typeNode);
            Logger.INSTANCE.debugln("%s depends on %s", node.getValue().getQualifiedName(), type.getQualifiedName());
        }
    }

    @Override
    public void enterTypeAlias(final TypeAliasContext context) {
        final var name = scopeStack.getScopeName().join(Utils.getIdentifier(context.ident()));
        if (checkIsAlreadyDefined(context, name)) {
            return;
        }
        // @formatter:off
        final var type = TypeUtils.getType(compiler, scopeStack, context.type())
            .unwrapOrReport(compiler, context.start, CompileStatus.TRANSLATION_ERROR);
        // @formatter:on
        if (type.isEmpty()) {
            return;
        }
        udts.put(name, Types.aliased(name, type.get()));
        super.enterTypeAlias(context);
    }

    @Override
    public void enterStruct(final StructContext context) {
        final var name = Utils.getIdentifier(context.ident());
        if (checkIsAlreadyDefined(context, name)) {
            return;
        }
        analyzeFieldLayout(context, name, UDTKind.STRUCT);
        super.enterStruct(context);
    }

    @Override
    public void enterClass(final ClassContext context) {
        final var name = Utils.getIdentifier(context.ident());
        if (checkIsAlreadyDefined(context, name)) {
            return;
        }
        analyzeFieldLayout(context, name, UDTKind.CLASS);
        super.enterClass(context);
    }

    @Override
    public void enterEnumClass(final EnumClassContext context) {
        final var name = Utils.getIdentifier(context.ident());
        if (checkIsAlreadyDefined(context, name)) {
            return;
        }
        analyzeFieldLayout(context, name, UDTKind.ENUM_CLASS);
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(final TraitContext context) {
        final var name = Utils.getIdentifier(context.ident());
        if (checkIsAlreadyDefined(context, name)) {
            return;
        }
        analyzeFieldLayout(context, name, UDTKind.TRAIT);
        super.enterTrait(context);
    }

    private boolean checkIsAlreadyDefined(final ParserRuleContext context, final Identifier name) {
        final var type = findTypeInScope(name, scopeStack.getScopeName());
        if (type != null) {
            final var compileContext = compiler.getContext();
            final var message = Utils.makeCompilerMessage(String.format("Type '%s' is already defined", name));
            compileContext.reportError(compileContext.makeError(context.start, message), CompileStatus.SEMANTIC_ERROR);
            return true;
        }
        return false;
    }

    public @Nullable Type findTypeInScope(final Identifier name, final Identifier scopeName) {
        return ScopeUtils.findInScope(udts, name, scopeName);
    }

    private void resolveFieldTypes(final UDT udt, final Identifier scopeName) {
        final var type = udt.structureType();
        final var fieldTypes = type.getFieldTypes();
        final var numFields = fieldTypes.size();
        for (var i = 0; i < numFields; i++) {
            final var fieldType = fieldTypes.get(i);
            if (fieldType.isBuiltin() || fieldType.isComplete()) {
                continue;
            }
            final var fieldTypeName = fieldType.getQualifiedName();
            Logger.INSTANCE.debugln("Found incomplete field type '%s' in '%s'", fieldTypeName, scopeName);
            final var completeType = findTypeInScope(fieldTypeName, scopeName);
            if (completeType == null) {
                final var message = String.format("Failed to resolve field type '%s' for '%s'", fieldTypeName,
                                                  scopeName);
                compiler.getContext().reportError(new CompileError(Utils.makeCompilerMessage(message)),
                                                  CompileStatus.ANALYZER_ERROR);
                continue;
            }
            Logger.INSTANCE.debugln("   Resolved to complete type '%s'", completeType.getQualifiedName());
            type.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
    }

    private void resolveFieldTypes() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            if (udt instanceof AliasedType alias) {
                // TODO: resolve type aliases
                continue;
            }
            resolveFieldTypes((UDT) udt, udt.getQualifiedName());
        }
    }

    private void materializeTypes() {
        for (final var udt : udts.values()) {
            udt.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materializing type %s", udt.getQualifiedName());
        }
    }

    private void sortTypes() {
        final var rootNode = new TopoNode<Type>(UDT.NULL); // Dummy UDT; TODO: improve this
        // @formatter:off
        final var nodes = udts.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        for (final var nodeEntry : nodes.entrySet()) {
            final var node = nodeEntry.getValue();
            final var type = node.getValue();
            if (type == null) {
                continue; // Skip sorting
            }
            final var queue = new ArrayDeque<Type>();
            queue.push(type);
            addTypesToGraph(queue, node, nodes);
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d UDT entries", udts.size());
        final var sortedNodes = new TopoSorter<>(rootNode).sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, Type>();

        for (final var node : sortedNodes) {
            final var type = node.getValue();
            if (type == null) {
                continue;
            }
            final var qualifiedName = type.getQualifiedName();
            sortedMap.put(qualifiedName, udts.get(qualifiedName));
        }

        udts.clear();
        udts.putAll(sortedMap);
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final Identifier name, final UDTKind udtKind) {
        final var layoutAnalyzer = new FieldLayoutAnalyzer(compiler); // Copy scope stack
        ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

        final var fieldTypes = layoutAnalyzer.getFields().stream().map(Field::getType).toArray(Type[]::new);
        final var type = scopeStack.applyEnclosingScopes(Types.structure(name, fieldTypes));

        final var udt = new UDT(udtKind, type, DefaultAccess.PRIVATE);
        udts.put(type.getQualifiedName(), udt);
        Logger.INSTANCE.debugln("Captured field layout for type '%s'", udt.structureType().getQualifiedName());
    }

    public void preProcessTypes() {
        try {
            sortTypes();
            resolveFieldTypes();
            materializeTypes();
        }
        catch (Throwable error) {
            final var message = Utils.makeCompilerMessage(
                    String.format("Could not pre-process types: %s", error.getMessage()));
            compiler.getContext().reportError(new CompileError(message), CompileStatus.ANALYZER_ERROR);
        }
    }
}

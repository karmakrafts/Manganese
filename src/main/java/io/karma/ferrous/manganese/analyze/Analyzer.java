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
import io.karma.ferrous.manganese.ocm.type.StructureType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.UDT;
import io.karma.ferrous.manganese.ocm.type.UDTKind;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;
import io.karma.ferrous.vanadium.FerrousParser.TraitContext;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

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
    private final LinkedHashMap<Identifier, UDT> udts = new LinkedHashMap<>();

    public Analyzer(Compiler compiler) {
        super(compiler);
    }

    public @Nullable UDT findUDTInScope(final Identifier name, final Identifier scopeName) {
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
            final var fieldTypeName = fieldType.getInternalName();
            Logger.INSTANCE.debugln("Found incomplete field type '%s' in '%s'", fieldTypeName, scopeName);
            final var completeUdt = findUDTInScope(fieldTypeName, scopeName);
            if (completeUdt == null) {
                final var message = String.format("Failed to resolve field type '%s' for '%s'", fieldTypeName,
                                                  scopeName);
                compiler.getContext().reportError(new CompileError(Utils.makeCompilerMessage(message)),
                                                  CompileStatus.ANALYZER_ERROR);
                continue;
            }
            final var completeType = completeUdt.structureType();
            if (completeType == null) {
                final var message = String.format("Failed to resolve field type '%s' for '%s'", fieldTypeName,
                                                  scopeName);
                compiler.getContext().reportError(new CompileError(Utils.makeCompilerMessage(message)),
                                                  CompileStatus.ANALYZER_ERROR);
                continue;
            }
            Logger.INSTANCE.debugln("   Resolved to complete type '%s'", completeType.getInternalName());
            type.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
    }

    private void resolveFieldTypes() {
        final var udts = this.udts.values();
        for (final var udt : udts) {
            resolveFieldTypes(udt, udt.structureType().getInternalName());
        }
    }

    private void materializeTypes() {
        for (final var udt : udts.values()) {
            final var type = udt.structureType();
            type.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materializing type %s", type.getInternalName());
        }
    }

    private void addTypesToGraph(final Type type, final TopoNode<UDT> node,
                                 final Map<Identifier, TopoNode<UDT>> nodes) {
        if (type instanceof StructureType struct) {
            final var fieldTypes = struct.getFieldTypes();
            for (final var fieldType : fieldTypes) {
                addTypesToGraph(fieldType, node, nodes);
            }
            return;
        }
        var typeNode = nodes.get(type.getInternalName());
        if (typeNode == null) {
            final var enclosingName = node.getValue().structureType().getInternalName();
            typeNode = nodes.get(enclosingName.join(type.getInternalName(), '.'));
            if (typeNode == null) {
                return;
            }
        }
        node.addDependency(typeNode);
        Logger.INSTANCE.debugln("%s depends on %s", node.getValue().structureType().getInternalName(),
                                type.getInternalName());
    }

    private void sortTypes() {
        final var rootNode = new TopoNode<>(UDT.NULL); // Dummy UDT; TODO: improve this
        // @formatter:off
        final var nodes = udts.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        for (final var nodeEntry : nodes.entrySet()) {
            final var node = nodeEntry.getValue();
            final var type = node.getValue().structureType();
            if (type == null) {
                continue; // Skip sorting
            }
            addTypesToGraph(type, node, nodes);
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d UDT entries", udts.size());
        final var sortedNodes = new TopoSorter<>(rootNode).sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, UDT>();

        for (final var node : sortedNodes) {
            final var type = node.getValue().structureType();
            if (type == null) {
                continue;
            }
            final var internalName = type.getInternalName();
            sortedMap.put(internalName, udts.get(internalName));
        }

        udts.clear();
        udts.putAll(sortedMap);
    }

    private void analyzeFieldLayout(final ParserRuleContext parent, final IdentContext identContext,
                                    final UDTKind udtKind) {
        final var name = Utils.getIdentifier(identContext);

        final var layoutAnalyzer = new FieldLayoutAnalyzer(compiler); // Copy scope stack
        ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

        final var fieldTypes = layoutAnalyzer.getFields().stream().map(Field::getType).toArray(Type[]::new);
        final var type = scopeStack.applyEnclosingScopes(Types.structure(name, fieldTypes));

        final var udt = new UDT(udtKind, type, DefaultAccess.PRIVATE);
        udts.put(type.getInternalName(), udt);
        Logger.INSTANCE.debugln("Captured field layout for type '%s'", udt.structureType().getInternalName());
    }

    public void preProcessTypes() {
        try {
            sortTypes();
            resolveFieldTypes();
            materializeTypes();
        }
        catch (Throwable error) {
            compiler.getContext()
                    .reportError(new CompileError(String.format("Could not pre-process types: %s", error.getMessage())),
                                 CompileStatus.ANALYZER_ERROR);
        }
    }

    @Override
    public void enterStruct(final StructContext context) {
        final var identifier = context.ident();
        analyzeFieldLayout(context, identifier, UDTKind.STRUCT);
        super.enterStruct(context);
    }

    @Override
    public void enterClass(final ClassContext context) {
        analyzeFieldLayout(context, context.ident(), UDTKind.CLASS);
        super.enterClass(context);
    }

    @Override
    public void enterEnumClass(final EnumClassContext context) {
        analyzeFieldLayout(context, context.ident(), UDTKind.ENUM_CLASS);
        super.enterEnumClass(context);
    }

    @Override
    public void enterTrait(final TraitContext context) {
        analyzeFieldLayout(context, context.ident(), UDTKind.TRAIT);
        super.enterTrait(context);
    }

    public LinkedHashMap<Identifier, UDT> getUDTs() {
        return udts;
    }
}

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

package io.karma.ferrous.manganese.compiler.pass;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.module.ModuleData;
import io.karma.ferrous.manganese.ocm.access.AccessKind;
import io.karma.ferrous.manganese.ocm.access.ScopedAccess;
import io.karma.ferrous.manganese.ocm.type.*;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.ScopeUtils;
import io.karma.kommons.topo.TopoNode;
import io.karma.kommons.topo.TopoSorter;
import io.karma.kommons.tuple.Pair;
import org.apiguardian.api.API;
import org.fusesource.jansi.Ansi;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class TypeResolutionPass implements CompilePass {
    @Override
    public void run(final Compiler compiler, final CompileContext compileContext, final Module module,
                    final ExecutorService executor) {
        Profiler.INSTANCE.push();
        final var moduleData = compileContext.getOrCreateModuleData(module.getName());
        sortTypes(compileContext, moduleData);
        resolveTypes(compileContext, moduleData);
        materializeTypes(compiler, compileContext, moduleData);
        resolveTypeAccess(compileContext, moduleData);
        Profiler.INSTANCE.pop();
    }

    private boolean addTypesToGraph(final CompileContext compileContext,
                                    final ArrayDeque<Pair<TopoNode<Type>, TopoNode<Type>>> typesToResolve,
                                    final Map<Identifier, TopoNode<Type>> nodes, final HashSet<Identifier> resolved) {
        final var buffer = Ansi.ansi().fgBright(Ansi.Color.CYAN);
        final var pair = typesToResolve.pop();

        final var parentNode = pair.getLeft();
        final var childNode = pair.getRight();
        final var childType = childNode.getValue();

        if (childType instanceof UserDefinedType udt && !udt.isComplete()) {
            buffer.fgBright(Ansi.Color.MAGENTA).a('U').fgBright(Ansi.Color.CYAN);
            final var fields = udt.fields();
            for (final var field : fields) {
                final var fieldType = field.getType();
                if (fieldType.isBuiltin() || fieldType.isComplete()) {
                    continue; // Skip all types which are complete to save time
                }
                var fieldTypeName = fieldType.getQualifiedName();
                final var fieldNode = ScopeUtils.findInScope(nodes, fieldTypeName, childType.getScopeName());
                if (fieldNode == null) {
                    final var token = field.getTokenSlice().findTokenOrFirst(fieldTypeName.toString());
                    compileContext.reportError(token, CompileErrorCode.E3004);
                    return false;
                }
                fieldTypeName = fieldNode.getValue().getQualifiedName();
                if (resolved.contains(fieldTypeName)) {
                    buffer.fgBright(Ansi.Color.GREEN).a('C').fgBright(Ansi.Color.CYAN);
                    continue; // Prevent multiple passes over the same kind
                }
                typesToResolve.push(Pair.of(childNode, fieldNode));
                resolved.add(fieldTypeName);
                buffer.a('R');
            }
        }

        if (childType instanceof AliasedType alias && !alias.isComplete()) {
            buffer.fgBright(Ansi.Color.MAGENTA).a('A').fgBright(Ansi.Color.CYAN);
            if (!alias.isBuiltin() && !alias.isComplete()) {
                final var backingType = alias.getBackingType();
                var backingTypeName = backingType.getQualifiedName();
                final var typeNode = ScopeUtils.findInScope(nodes, backingTypeName, childType.getScopeName());
                if (typeNode == null) {
                    compileContext.reportError(backingTypeName.toString(), CompileErrorCode.E3004);
                    return false;
                }
                backingTypeName = typeNode.getValue().getQualifiedName();
                if (!resolved.contains(backingTypeName)) { // Prevent multiple passes over the same kind
                    typesToResolve.push(Pair.of(childNode, typeNode));
                    resolved.add(backingTypeName);
                    buffer.a('R');
                }
                else {
                    buffer.fgBright(Ansi.Color.GREEN).a('C').fgBright(Ansi.Color.CYAN);
                }
            }
        }

        parentNode.addDependency(childNode);
        buffer.a('D').fg(Ansi.Color.CYAN).a(" > ").a(childType);
        Logger.INSTANCE.debugln("Resolved kind graph: %s", buffer.toString());

        return true;
    }

    private boolean resolveAliasedType(final AliasedType alias, final Identifier scopeName,
                                       final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        var currentType = alias.getBackingType();
        while (!currentType.isComplete()) {
            if (currentType instanceof AliasedType aliasedType) {
                currentType = aliasedType.getBackingType();
                continue;
            }
            final var currentTypeName = currentType.getQualifiedName();
            final var completeType = moduleData.findCompleteType(currentTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            currentType = completeType;
        }
        alias.setBackingType(currentType);
        Profiler.INSTANCE.pop();
        return true;
    }

    private boolean resolveFieldTypes(final CompileContext compileContext, final UserDefinedType type,
                                      final Identifier scopeName, final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        final var structType = type.type();
        final var fieldTypes = structType.getFieldTypes();
        final var numFields = fieldTypes.size();
        for (var i = 0; i < numFields; i++) {
            final var fieldType = fieldTypes.get(i);
            if (fieldType.isBuiltin() || fieldType.isComplete()) {
                continue;
            }
            final var fieldTypeName = fieldType.getQualifiedName();
            Logger.INSTANCE.debugln("Found incomplete field kind '%s' in '%s'", fieldTypeName, scopeName);
            final var completeType = moduleData.findCompleteType(fieldTypeName, scopeName);
            if (completeType == null) {
                return false;
            }
            Logger.INSTANCE.debugln("  > Resolved to complete kind '%s'", completeType);
            structType.setFieldType(i, completeType.derive(fieldType.getAttributes()));
        }
        Profiler.INSTANCE.pop();
        return true;
    }

    private void resolveTypes(final CompileContext compileContext, final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        final var types = moduleData.getTypes().values();
        for (final var udt : types) {
            final var scopeName = udt.getScopeName();
            if (udt.isAliased() && udt instanceof AliasedType alias) {
                if (!resolveAliasedType(alias, scopeName, moduleData)) {
                    compileContext.reportError(alias.getTokenSlice().getFirstToken(), CompileErrorCode.E3003);
                }
            }
            if (!(udt instanceof UserDefinedType actualUdt)) {
                continue; // Skip everything else apart from UDTs
            }
            if (!resolveFieldTypes(compileContext, actualUdt, scopeName, moduleData)) {
                compileContext.reportError(actualUdt.tokenSlice().getFirstToken(), CompileErrorCode.E3004);
            }
        }
        Profiler.INSTANCE.pop();
    }

    private void materializeTypes(final Compiler compiler, final CompileContext compileContext,
                                  final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        final var namedTypes = moduleData.getTypes().values();
        for (final var type : namedTypes) {
            if (type.isAliased()) {
                continue; // Don't need to waste time on doing nothing..
            }
            if (!type.isComplete()) {
                Logger.INSTANCE.errorln("Cannot materialize kind '%s' as it is incomplete", type.getQualifiedName());
                continue;
            }
            final var address = type.materialize(compiler.getTargetMachine());
            Logger.INSTANCE.debugln("Materialized kind %s at 0x%08X", type.getQualifiedName(), address);
        }
        Profiler.INSTANCE.pop();
    }

    private void sortTypes(final CompileContext compileContext, final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        final var rootNode = new TopoNode<Type>(DummyType.INSTANCE);
        final var namedTypes = moduleData.getTypes();
        // @formatter:off
        final var nodes = namedTypes.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey(), new TopoNode<>(e.getValue())))
            .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        // @formatter:on

        final var queue = new ArrayDeque<Pair<TopoNode<Type>, TopoNode<Type>>>(); // <parent_node, child_node>
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
                if (!addTypesToGraph(compileContext, queue, nodes, resolved)) {
                    break outer;
                }
            }
            rootNode.addDependency(node);
        }

        Logger.INSTANCE.debugln("Reordering %d kind entries", namedTypes.size());
        final var sortedNodes = new TopoSorter<>(rootNode).sort(ArrayList::new);
        final var sortedMap = new LinkedHashMap<Identifier, Type>();

        for (final var node : sortedNodes) {
            final var type = node.getValue();
            if (type == null || type == NullType.INSTANCE || type == DummyType.INSTANCE) {
                continue;
            }
            final var qualifiedName = type.getQualifiedName();
            sortedMap.put(qualifiedName, namedTypes.get(qualifiedName));
        }

        namedTypes.clear();
        namedTypes.putAll(sortedMap);
        Profiler.INSTANCE.pop();
    }

    private void resolveTypeAccess(final CompileContext compileContext, final ModuleData moduleData) {
        Profiler.INSTANCE.push();
        final var namedTypes = moduleData.getTypes().values();
        for (final var udt : namedTypes) {
            if (!(udt instanceof UserDefinedType actualUdt)) {
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
                    if (type.isComplete()) {
                        continue;
                    }
                    final var completeType = moduleData.findCompleteType(type);
                    if (completeType == null) {
                        final var token = scopedAccess.tokenSlice().findTokenOrFirst(type.getQualifiedName().toString());
                        compileContext.reportError(token, CompileErrorCode.E3002);
                        continue;
                    }
                    types[i] = completeType;
                    Logger.INSTANCE.debugln("Resolved scoped access kind '%s'", completeType);
                }
            }
        }
        Profiler.INSTANCE.pop();
    }
}

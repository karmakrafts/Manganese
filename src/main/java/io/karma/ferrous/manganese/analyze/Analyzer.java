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

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.ocm.Function;
import io.karma.ferrous.manganese.ocm.Type;
import io.karma.ferrous.manganese.ocm.Types;
import io.karma.ferrous.manganese.ocm.UDT;
import io.karma.ferrous.manganese.ocm.UDTType;
import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.scope.ScopeStack;
import io.karma.ferrous.manganese.scope.ScopeType;
import io.karma.ferrous.manganese.util.FunctionUtils;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.ClassContext;
import io.karma.ferrous.vanadium.FerrousParser.EnumClassContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.InterfaceContext;
import io.karma.ferrous.vanadium.FerrousParser.ProtoFunctionContext;
import io.karma.ferrous.vanadium.FerrousParser.StructContext;
import io.karma.ferrous.vanadium.FerrousParser.TraitContext;
import io.karma.kommons.topo.TopoNode;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.HashMap;

/**
 * Special translation unit ran during the pre-compilation pass
 * to discover declarations throughout the code.
 *
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public final class Analyzer extends ParseAdapter {
    private final HashMap<Identifier, Function> functions = new HashMap<>();
    private final HashMap<Identifier, UDT> udts = new HashMap<>();
    private final ScopeStack scopes = new ScopeStack();

    public Analyzer(Compiler compiler) {
        super(compiler);
        scopes.push(Scope.GLOBAL); // Start with global scope
    }

    /**
     * Applies a topological sort to all UDTs, so they can be
     * properly pre-materialized in the correct order.
     * This means that no cyclic struct declarations are possible,
     * like in most other native and non-native languages.
     */
    public void preMaterialize() {
        // @formatter:off
        final var nodes = udts.values()
            .stream()
            .map(TopoNode::new)
            .toList();
        // @formatter:on
    }

    private void parseFieldLayout(final ParserRuleContext parent, final IdentContext identContext,
                                  final UDTType udtType) {
        final var name = scopes.getNestedName().join(Utils.getIdentifier(identContext), '.');
        final var parser = new FieldLayoutAnalyzer(compiler);
        ParseTreeWalker.DEFAULT.walk(parser, parent);
        final var type = Types.structure(name, parser.getFields().stream().map(Field::type).toArray(Type[]::new));
        final var udt = new UDT(udtType, type);
        udts.put(name, udt);
        Logger.INSTANCE.debugln("Captured field layout '%s'", udt);
    }

    @Override
    public void enterStruct(StructContext context) {
        final var identifier = context.ident();
        parseFieldLayout(context, identifier, UDTType.STRUCT);
        scopes.push(new Scope(ScopeType.STRUCT, Utils.getIdentifier(identifier)));
    }

    @Override
    public void exitStruct(StructContext context) {
        scopes.pop();
    }

    @Override
    public void enterClass(ClassContext context) {
        final var identifier = context.ident();
        parseFieldLayout(context, identifier, UDTType.CLASS);
        scopes.push(new Scope(ScopeType.CLASS, Utils.getIdentifier(identifier)));
    }

    @Override
    public void exitClass(ClassContext context) {
        scopes.pop();
    }

    @Override
    public void enterEnumClass(EnumClassContext context) {
        final var identifier = context.ident();
        parseFieldLayout(context, identifier, UDTType.ENUM_CLASS);
        scopes.push(new Scope(ScopeType.ENUM_CLASS, Utils.getIdentifier(identifier)));
    }

    @Override
    public void exitEnumClass(EnumClassContext context) {
        scopes.pop();
    }

    @Override
    public void enterTrait(TraitContext context) {
        final var identifier = context.ident();
        parseFieldLayout(context, identifier, UDTType.TRAIT);
        scopes.push(new Scope(ScopeType.TRAIT, Utils.getIdentifier(identifier)));
    }

    @Override
    public void exitTrait(TraitContext context) {
        scopes.pop();
    }

    @Override
    public void enterInterface(InterfaceContext context) {
        // TODO: Implement interface analysis
        scopes.push(new Scope(ScopeType.INTERFACE, Utils.getIdentifier(context.ident())));
    }

    @Override
    public void exitInterface(InterfaceContext context) {
        scopes.pop();
    }

    // TODO: Analyze attributes

    @Override
    public void enterProtoFunction(ProtoFunctionContext context) {
        final var name = FunctionUtils.getFunctionName(context.functionIdent());
        final var type = TypeUtils.getFunctionType(compiler, context);
        final var function = new Function(name, type);
        functions.put(name, function);
        Logger.INSTANCE.debugln("Found function '%s'", function);
    }

    public HashMap<Identifier, Function> getFunctions() {
        return functions;
    }

    public HashMap<Identifier, UDT> getUDTs() {
        return udts;
    }
}

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
import io.karma.ferrous.manganese.ocm.field.Field;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
import io.karma.ferrous.manganese.ocm.type.UserDefinedTypeKind;
import io.karma.ferrous.manganese.parser.FieldParser;
import io.karma.ferrous.manganese.parser.ParseAdapter;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class TypeDiscoveryPass implements CompilePass {
    @Override
    public void run(final Compiler compiler, final CompileContext compileContext, final Module module,
                    final ExecutorService executor) {
        Profiler.INSTANCE.push();
        final var moduleData = compileContext.getOrCreateModuleData(module.getName());
        compileContext.walkParseTree(new ParseListenerImpl(compiler, compileContext, moduleData));
        Profiler.INSTANCE.pop();
    }

    private static final class ParseListenerImpl extends ParseAdapter {
        private final ModuleData moduleData;

        public ParseListenerImpl(final Compiler compiler, final CompileContext compileContext,
                                 final ModuleData moduleData) {
            super(compiler, compileContext);
            this.moduleData = moduleData;
        }

        @Override
        public void enterTypeAlias(final TypeAliasContext context) {
            final var identContext = context.ident();
            if (checkIsTypeAlreadyDefined(identContext)) {
                return;
            }
            final var type = Types.parse(compiler, compileContext, scopeStack, context.type());
            final var genericParams = GenericParameter.parse(compiler,
                compileContext,
                scopeStack,
                context.genericParamList());
            final var aliasedType = Types.aliased(Identifier.parse(identContext),
                type,
                scopeStack::applyEnclosingScopes,
                TokenSlice.from(compileContext, context),
                genericParams);
            moduleData.getTypes().put(aliasedType.getQualifiedName(), aliasedType);
        }

        @Override
        public void enterAttrib(AttribContext context) {
            final var identContext = context.ident();
            if (checkIsTypeAlreadyDefined(identContext)) {
                return;
            }
            final var genericParams = GenericParameter.parse(compiler,
                compileContext,
                scopeStack,
                context.genericParamList());

            super.enterAttrib(context);
            final var scope = analyzeFieldLayout(context,
                Identifier.parse(identContext),
                genericParams,
                UserDefinedTypeKind.ATTRIBUTE);
            popScope();
            pushScope(scope);
        }

        @Override
        public void enterStruct(final StructContext context) {
            final var identContext = context.ident();
            if (checkIsTypeAlreadyDefined(identContext)) {
                return;
            }
            final var genericParams = GenericParameter.parse(compiler,
                compileContext,
                scopeStack,
                context.genericParamList());

            super.enterStruct(context);
            final var scope = analyzeFieldLayout(context,
                Identifier.parse(identContext),
                genericParams,
                UserDefinedTypeKind.STRUCT);
            popScope();
            pushScope(scope);
        }

        @Override
        public void enterEnumClass(final EnumClassContext context) {
            final var identContext = context.ident();
            if (checkIsTypeAlreadyDefined(identContext)) {
                return;
            }

            super.enterEnumClass(context);
            final var scope = analyzeFieldLayout(context,
                Identifier.parse(identContext),
                Collections.emptyList(),
                UserDefinedTypeKind.ENUM_CLASS);
            popScope();
            pushScope(scope);
        }

        @Override
        public void enterTrait(final TraitContext context) {
            final var identContext = context.ident();
            if (checkIsTypeAlreadyDefined(identContext)) {
                return;
            }
            final var genericParams = GenericParameter.parse(compiler,
                compileContext,
                scopeStack,
                context.genericParamList());

            super.enterTrait(context);
            final var scope = analyzeFieldLayout(context,
                Identifier.parse(identContext),
                genericParams,
                UserDefinedTypeKind.TRAIT);
            popScope();
            pushScope(scope);
        }

        private boolean checkIsTypeAlreadyDefined(final IdentContext identContext) {
            final var name = Identifier.parse(identContext);
            final var type = moduleData.getTypes().get(name);
            if (type != null) {
                final var message = KitchenSink.makeCompilerMessage(String.format("Type '%s' is already defined",
                    name));
                compileContext.reportError(identContext.start, message, CompileErrorCode.E3000);
                return true;
            }
            return false;
        }

        private UserDefinedType analyzeFieldLayout(final ParserRuleContext parent, final Identifier name,
                                                   final List<GenericParameter> genericParams,
                                                   final UserDefinedTypeKind kind) {
            final var layoutAnalyzer = new FieldParser(compiler, compileContext, scopeStack);
            ParseTreeWalker.DEFAULT.walk(layoutAnalyzer, parent);

            final var fields = layoutAnalyzer.getFields();
            final var fieldTypes = fields.stream().map(Field::getType).toList();
            final var tokenSlice = TokenSlice.from(compileContext, parent);
            final var type = Types.structure(name,
                scopeStack::applyEnclosingScopes,
                genericParams,
                tokenSlice,
                fieldTypes);
            final var udt = new UserDefinedType(kind, type, fields, tokenSlice);
            moduleData.getTypes().put(type.getQualifiedName(), udt);

            Logger.INSTANCE.debugln("Captured field layout for type '%s'", type.getQualifiedName());
            return udt;
        }
    }
}

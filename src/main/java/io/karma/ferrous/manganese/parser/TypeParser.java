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

import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeAttribute;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class TypeParser extends ParseAdapter {
    private final Stack<TypeAttribute> attributes = new Stack<>();
    private final ScopeStack capturedScopeStack;
    private Type baseType;

    public TypeParser(final Compiler compiler, final CompileContext compileContext,
                      final ScopeStack capturedScopeStack) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    @Override
    public void enterPointerType(final PointerTypeContext context) {
        attributes.push(TypeAttribute.POINTER);
        super.enterPointerType(context);
    }

    @Override
    public void enterRefType(final RefTypeContext context) {
        if (attributes.contains(TypeAttribute.REFERENCE)) {
            compileContext.reportError(context.start, CompileErrorCode.E3001);
            return;
        }
        attributes.push(TypeAttribute.REFERENCE);
        super.enterRefType(context);
    }

    @Override
    public void enterIdent(final IdentContext context) {
        if (baseType != null) {
            return; // Qualified ident contains these, so if kind exists, skip
        }
        final var name = Identifier.parse(context);
        final var type = compileContext.getPreAnalyzer().findCompleteTypeInScope(name,
            capturedScopeStack.getScopeName());
        if (type == null) {
            baseType = Types.incomplete(name,
                capturedScopeStack::applyEnclosingScopes,
                TokenSlice.from(compileContext, context));
            return;
        }
        baseType = type;
        super.enterIdent(context);
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        final var name = Identifier.parse(context);
        final var type = compileContext.getPreAnalyzer().findCompleteTypeInScope(name,
            capturedScopeStack.getScopeName());
        if (type == null) {
            baseType = Types.incomplete(name,
                capturedScopeStack::applyEnclosingScopes,
                TokenSlice.from(compileContext, context));
            return;
        }
        baseType = type;
        super.enterQualifiedIdent(context);
    }

    @Override
    public void enterMiscType(final MiscTypeContext context) {
        parsePrimitiveType(context, "Unknown miscellaneous kind");
        super.enterMiscType(context);
    }

    @Override
    public void enterSintType(final SintTypeContext context) {
        parsePrimitiveType(context, "Unknown signed integer kind");
        super.enterSintType(context);
    }

    @Override
    public void enterUintType(final UintTypeContext context) {
        parsePrimitiveType(context, "Unknown unsigned integer kind");
        super.enterUintType(context);
    }

    @Override
    public void enterFloatType(final FloatTypeContext context) {
        parsePrimitiveType(context, "Unknown floating point kind");
        super.enterFloatType(context);
    }

    private void parsePrimitiveType(final ParserRuleContext context, final String errorMessage) {
        final var text = context.getText();
        if (baseType != null) {
            Logger.INSTANCE.warnln("Base kind was already parsed");
            return;
        }
        final var type = Types.builtin(Identifier.parse(text));
        if (type.isEmpty()) {
            Logger.INSTANCE.errorln("Could not parse primitive kind '%s'", text);
            return;
        }
        baseType = type.get();
    }

    public Type getType() {
        if (attributes.isEmpty()) {
            return baseType;
        }
        return baseType.derive(attributes.toArray(TypeAttribute[]::new));
    }
}

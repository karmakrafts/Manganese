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

package io.karma.ferrous.manganese.translate;

import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeAttribute;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.ScopeStack;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.FloatTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.MiscTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.PointerTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.QualifiedIdentContext;
import io.karma.ferrous.vanadium.FerrousParser.RefTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.SintTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.SliceTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.UintTypeContext;
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

    public TypeParser(final Compiler compiler, final ScopeStack capturedScopeStack) {
        super(compiler);
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
            compiler.reportError(compiler.makeError(context.start, Utils.makeCompilerMessage(
                    "Type can only have one level of reference")), CompileStatus.SEMANTIC_ERROR);
            return;
        }
        attributes.push(TypeAttribute.REFERENCE);
        super.enterRefType(context);
    }

    @Override
    public void enterSliceType(final SliceTypeContext context) {
        attributes.push(TypeAttribute.SLICE);
        super.enterSliceType(context);
    }

    // Actual types

    private void parsePrimitiveType(final ParserRuleContext context, final String errorMessage) {
        final var text = context.getText();
        if (baseType != null) {
            compiler.reportError(compiler.makeError(context.start, errorMessage), CompileStatus.TRANSLATION_ERROR);
            return;
        }
        final var opt = Types.builtin(Identifier.parse(text));
        if (opt.isEmpty()) {
            compiler.reportError(compiler.makeError(context.start, "Invalid builtin type"),
                                 CompileStatus.TRANSLATION_ERROR);
            return;
        }
        baseType = opt.get();
    }

    @Override
    public void enterIdent(final IdentContext context) {
        final var name = Utils.getIdentifier(context);
        final var udt = compiler.getAnalyzer().findUDTInScope(name, capturedScopeStack.getScopeName());
        if (udt == null) {
            baseType = Types.incomplete(name);
            return;
        }
        baseType = udt.structureType();
        super.enterIdent(context);
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        final var name = Utils.getIdentifier(context);
        final var udt = compiler.getAnalyzer().findUDTInScope(name, capturedScopeStack.getScopeName());
        if (udt == null) {
            baseType = Types.incomplete(name);
            return;
        }
        baseType = udt.structureType();
        super.enterQualifiedIdent(context);
    }

    @Override
    public void enterMiscType(final MiscTypeContext context) {
        parsePrimitiveType(context, "Unknown miscellaneous type");
        super.enterMiscType(context);
    }

    @Override
    public void enterSintType(final SintTypeContext context) {
        parsePrimitiveType(context, "Unknown signed integer type");
        super.enterSintType(context);
    }

    @Override
    public void enterUintType(final UintTypeContext context) {
        parsePrimitiveType(context, "Unknown unsigned integer type");
        super.enterUintType(context);
    }

    @Override
    public void enterFloatType(final FloatTypeContext context) {
        parsePrimitiveType(context, "Unknown floating point type");
        super.enterFloatType(context);
    }

    public Type getType() {
        if (attributes.isEmpty()) {
            return baseType;
        }
        return baseType.derive(attributes.toArray(TypeAttribute[]::new));
    }
}

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

import io.karma.ferrous.manganese.CompileError;
import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.type.Type;
import io.karma.ferrous.manganese.type.TypeAttribute;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.*;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Stack;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TypeTranslationUnit extends AbstractTranslationUnit {
    private final Stack<TypeAttribute> attributes = new Stack<>();
    private Type baseType;

    public TypeTranslationUnit(Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterPointerType(PointerTypeContext context) {
        attributes.push(TypeAttribute.POINTER);
    }

    @Override
    public void enterRefType(RefTypeContext context) {
        if (attributes.contains(TypeAttribute.REFERENCE)) {
            final var error = new CompileError(context.start);
            error.setAdditionalText(Utils.makeCompilerMessage("Type can only have one level of reference", null));
            compiler.reportError(error, CompileStatus.SEMANTIC_ERROR);
            return;
        }
        attributes.push(TypeAttribute.REFERENCE);
    }

    @Override
    public void enterSliceType(SliceTypeContext arrayTypeContext) {
        attributes.push(TypeAttribute.SLICE);
    }

    // Actual types

    private void handleType(final ParserRuleContext context, final String errorMessage) {
        final var text = context.getText();
        doOrReport(context, () -> {
            if (baseType != null) {
                throw new TranslationException(context.start, "Type already translated, how did this happen?");
            }
            // @formatter:off
            baseType = Type.findBuiltinType(text)
                .orElseThrow(() -> new TranslationException(context.start, "%s: '%s'", errorMessage, text));
            // @formatter:on
        });
    }

    @Override
    public void enterMiscType(MiscTypeContext context) {
        handleType(context, "Unknown miscellaneous type");
    }

    @Override
    public void enterSintType(SintTypeContext context) {
        handleType(context, "Unknown signed integer type");
    }

    @Override
    public void enterUintType(UintTypeContext context) {
        handleType(context, "Unknown unsigned integer type");
    }

    @Override
    public void enterFloatType(FloatTypeContext context) {
        handleType(context, "Unknown floating point type");
    }

    public Type getType() {
        return baseType.derive(attributes.toArray(TypeAttribute[]::new));
    }
}

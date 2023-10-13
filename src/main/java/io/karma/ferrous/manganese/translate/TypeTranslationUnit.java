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
import io.karma.ferrous.manganese.type.Type;
import io.karma.ferrous.vanadium.FerrousParser.FloatTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.MiscTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.PointerTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.RefTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.SintTypeContext;
import io.karma.ferrous.vanadium.FerrousParser.UintTypeContext;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class TypeTranslationUnit extends AbstractTranslationUnit {
    private int pointerDepth = 0;
    private boolean isReference = false;
    private Type baseType;

    public TypeTranslationUnit(Compiler compiler) {
        super(compiler);
    }

    @Override
    public void enterPointerType(PointerTypeContext ctx) {
        pointerDepth++;
    }

    @Override
    public void exitPointerType(PointerTypeContext ctx) {
        pointerDepth--;
    }

    @Override
    public void enterRefType(RefTypeContext ctx) {
        isReference = true;
    }

    @Override
    public void exitRefType(RefTypeContext ctx) {
        isReference = false;
    }

    // Actual types

    private void handleType(final String text) {
        doOrReport(() -> baseType = Type.findBuiltinType(text).orElseThrow(), CompileStatus.TRANSLATION_ERROR);
    }

    @Override
    public void enterMiscType(MiscTypeContext ctx) {
        handleType(ctx.getText());
    }

    @Override
    public void enterSintType(SintTypeContext ctx) {
        handleType(ctx.getText());
    }

    @Override
    public void enterUintType(UintTypeContext ctx) {
        handleType(ctx.getText());
    }

    @Override
    public void enterFloatType(FloatTypeContext ctx) {
        handleType(ctx.getText());
    }

    public Type materializeType() {
        return baseType.derive(pointerDepth, isReference);
    }
}

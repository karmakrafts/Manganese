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

import io.karma.ferrous.manganese.CompileStatus;
import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.ParseAdapter;
import io.karma.ferrous.manganese.ocm.Field;
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.manganese.util.ScopeStack;
import io.karma.ferrous.manganese.util.TypeUtils;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.ferrous.vanadium.FerrousParser.FieldContext;
import io.karma.ferrous.vanadium.FerrousParser.UdtContext;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class FieldLayoutAnalyzer extends ParseAdapter {
    private final ArrayList<Field> fields = new ArrayList<>();
    private int nestedScopes = 0;

    public FieldLayoutAnalyzer(final Compiler compiler) {
        super(compiler);
    }

    public boolean isOutOfScope() {
        return nestedScopes > 0;
    }

    @Override
    public void enterUdt(UdtContext udtDeclContext) {
        nestedScopes++;
    }

    @Override
    public void exitUdt(UdtContext udtDeclContext) {
        nestedScopes--;
    }

    @Override
    public void enterField(final FieldContext context) {
        if (isOutOfScope()) {
            return;
        }
        compiler.doOrReport(context, () -> {
            final var name = Utils.getIdentifier(context.ident());
            // @formatter:off
            final var type = TypeUtils.getType(compiler, ScopeStack.EMPTY, context.type())
                .orElseThrow(() -> new TranslationException(context.start, "Unknown field type"));
            // @formatter:on
            fields.add(new Field(name, type));
        }, CompileStatus.TRANSLATION_ERROR);
        super.enterField(context);
    }

    public ArrayList<Field> getFields() {
        return fields;
    }
}

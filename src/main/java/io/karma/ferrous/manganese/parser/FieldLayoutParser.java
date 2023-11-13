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
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.field.Field;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.FieldContext;
import io.karma.ferrous.vanadium.FerrousParser.UdtContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.ArrayList;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public final class FieldLayoutParser extends ParseAdapter {
    private final ArrayList<Field> fields = new ArrayList<>();
    private final ScopeStack capturedScopeStack;
    private int nestedScopes = 0;

    public FieldLayoutParser(final Compiler compiler, final CompileContext compileContext,
                             final ScopeStack capturedScopeStack) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    public boolean isOutOfScope() {
        return nestedScopes > 0;
    }

    @Override
    public void enterUdt(final UdtContext context) {
        nestedScopes++;
    }

    @Override
    public void exitUdt(final UdtContext context) {
        nestedScopes--;
    }

    @Override
    public void enterField(final FieldContext context) {
        if (isOutOfScope()) {
            return;
        }
        fields.add(new Field(fields.size(),
            Identifier.parse(context.ident()),
            Types.parse(compiler, compileContext, capturedScopeStack, context.type()),
            KitchenSink.parseAccess(compiler, compileContext, scopeStack, context.accessMod()),
            KitchenSink.parseStorageMods(context.storageMod()),
            context.KW_MUT() != null,
            context.KW_STATIC() != null,
            capturedScopeStack.peek().getScopeType().isGlobal(),
            TokenSlice.from(compileContext, context)));
        super.enterField(context);
    }

    public ArrayList<Field> getFields() {
        return fields;
    }
}

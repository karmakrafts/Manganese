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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.ocm.AttributeUsage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
import io.karma.ferrous.manganese.ocm.type.UserDefinedTypeKind;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.QualifiedIdentContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apiguardian.api.API;

import java.util.LinkedHashMap;

/**
 * @author Alexander Hinze
 * @since 24/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class AttributeUsageParser extends ParseAdapter {
    private final ScopeStack capturedScopeStack;
    private final LinkedHashMap<Identifier, Expression> values = new LinkedHashMap<>();
    private UserDefinedType attribute;

    public AttributeUsageParser(final Compiler compiler, final CompileContext compileContext,
                                final ScopeStack capturedScopeStack) {
        super(compiler, compileContext);
        this.capturedScopeStack = capturedScopeStack;
    }

    private void parseType(final ParserRuleContext context) {
        if (attribute != null) {
            return;
        }
        final var foundType = compileContext.getOrCreateModuleData().findCompleteType(Identifier.parse(context),
            capturedScopeStack.getScopeName());
        if (!(foundType instanceof UserDefinedType udt)) {
            compileContext.reportError(context.start, CompileErrorCode.E3002);
            return;
        }
        if (udt.kind() != UserDefinedTypeKind.ATTRIBUTE) {
            compileContext.reportError(context.start, CompileErrorCode.E4016);
            return;
        }
        attribute = udt;
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        parseType(context);
        super.enterQualifiedIdent(context);
    }

    @Override
    public void enterIdent(final IdentContext context) {
        parseType(context);
        super.enterIdent(context);
    }

    public AttributeUsage getUsage() {
        return new AttributeUsage(attribute, values);
    }
}

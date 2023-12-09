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
import io.karma.ferrous.manganese.ocm.AttributeUsage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.UserDefinedType;
import io.karma.ferrous.manganese.ocm.type.UserDefinedTypeKind;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.vanadium.FerrousParser.ExprListContext;
import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.NamedExprListContext;
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

    public AttributeUsageParser(final CompileContext compileContext, final ScopeStack capturedScopeStack) {
        super(compileContext);
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
    public void enterExprList(final ExprListContext context) {
        final var expressions = ExpressionParser.parse(compileContext, capturedScopeStack, context, attribute);
        final var fields = attribute.fields();
        final var numFields = fields.size();
        for (var i = 0; i < numFields; i++) {
            values.put(fields.get(i).getName(), expressions.get(i));
        }
    }

    @Override
    public void enterNamedExprList(final NamedExprListContext context) {
        final var expressions = ExpressionParser.parseNamed(compileContext, capturedScopeStack, context, attribute);
        for (final var expr : expressions) {
            values.put(expr.getLeft(), expr.getRight());
        }
    }

    @Override
    public void enterQualifiedIdent(final QualifiedIdentContext context) {
        parseType(context);
    }

    @Override
    public void enterIdent(final IdentContext context) {
        parseType(context);
    }

    public AttributeUsage getUsage() {
        return new AttributeUsage(attribute, values);
    }
}

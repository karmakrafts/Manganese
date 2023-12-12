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

package io.karma.ferrous.manganese.ocm.function;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.parser.ExpressionParser;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.vanadium.FerrousParser.ParamContext;
import io.karma.ferrous.vanadium.FerrousParser.ParamListContext;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public final class Parameter implements Named {
    private final Identifier name;
    private final Expression defaultValue;
    private final boolean isMutable;
    private Type type;

    public Parameter(final Identifier name, final Type type, final boolean isMutable,
                     final @Nullable Expression defaultValue) {
        this.name = name;
        this.type = type;
        this.isMutable = isMutable;
        this.defaultValue = defaultValue;
    }

    public static @Nullable Parameter parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                            final @Nullable ParamContext context) {
        if (context == null) {
            return null;
        }
        final var name = Identifier.parse(context.ident());
        final var type = Types.parse(compileContext, scopeStack, context.type());
        if (type == null) {
            return null; // TODO: report error?
        }
        final var defaultValue = ExpressionParser.parse(compileContext, scopeStack, context.expr(), null);
        return new Parameter(name, type, context.KW_MUT() != null, defaultValue);
    }

    public static List<Parameter> parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                        final @Nullable ParamListContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        final var paramContexts = context.param();
        final var params = new ArrayList<Parameter>();
        for (final var paramContext : paramContexts) {
            params.add(parse(compileContext, scopeStack, paramContext));
        }
        return params;
    }

    public Type getType() {
        return type;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public @Nullable Expression getDefaultValue() {
        return defaultValue;
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Object

    @Override
    public String toString() {
        if (defaultValue == null) {
            return String.format("%s:%s", name, type);
        }
        return String.format("%s:%s=%s", name, type, defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, defaultValue);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Parameter param) { // @formatter:off
            return name.equals(param.name)
                && type.equals(param.type)
                && Objects.equals(defaultValue, param.defaultValue);
        } // @formatter:on
        return false;
    }
}

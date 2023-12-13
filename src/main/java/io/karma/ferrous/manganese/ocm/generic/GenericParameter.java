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

package io.karma.ferrous.manganese.ocm.generic;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.ocm.Named;
import io.karma.ferrous.manganese.ocm.constant.TypeConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.ScopeStack;
import io.karma.ferrous.manganese.ocm.type.Types;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.vanadium.FerrousParser.GenericParamContext;
import io.karma.ferrous.vanadium.FerrousParser.GenericParamListContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
public final class GenericParameter implements Named {
    private final Identifier name;
    private final GenericConstraint constraints;
    private Expression value;

    public GenericParameter(final Identifier name, final GenericConstraint constraints,
                            final @Nullable Expression defaultValue) {
        this.name = name;
        this.constraints = constraints;
        value = defaultValue;
    }

    public GenericParameter(final Identifier name) {
        this(name, GenericConstraint.TRUE, null);
    }

    public static @Nullable GenericParameter parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                                   final @Nullable GenericParamContext context) {
        if (context == null) {
            return null;
        }
        final var name = new Identifier(context.ident().getText());
        final var constraints = GenericConstraint.parse(compileContext, context.genericExpr());
        final var typeContext = context.type();
        final var defaultValue = Types.parse(compileContext, scopeStack, typeContext);
        return new GenericParameter(name,
            constraints,
            defaultValue != null ? new TypeConstant(defaultValue, TokenSlice.from(compileContext, typeContext)) : null);
    }

    public static List<GenericParameter> parse(final CompileContext compileContext, final ScopeStack scopeStack,
                                               final @Nullable GenericParamListContext context) {
        if (context == null) {
            return Collections.emptyList();
        }
        final var paramContexts = context.genericParam();
        final var params = new ArrayList<GenericParameter>(paramContexts.size()); // Pre-allocate
        for (final var paramContext : paramContexts) {
            params.add(parse(compileContext, scopeStack, paramContext));
        }
        return params;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    public GenericConstraint getConstraints() {
        return constraints;
    }

    public @Nullable Expression getValue() {
        return value;
    }

    public void setValue(final @Nullable Expression value) {
        this.value = value;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(name, constraints, value);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof GenericParameter param) { // @formatter:off
            return name.equals(param.name)
                && constraints.equals(param.constraints)
                && Objects.equals(value, param.value);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        if (value != null) {
            return STR."\{name}:\{constraints}=\{value}";
        }
        return STR."\{name}:\{constraints}";
    }
}

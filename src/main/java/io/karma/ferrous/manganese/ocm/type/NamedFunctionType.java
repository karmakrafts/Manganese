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

package io.karma.ferrous.manganese.ocm.type;

import io.karma.ferrous.manganese.ocm.scope.EnclosingScopeProvider;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public final class NamedFunctionType extends FunctionType {
    private final Identifier name;
    private EnclosingScopeProvider enclosingScope;

    NamedFunctionType(final Identifier name, final Type returnType, final boolean isVarArg, final Type... paramTypes) {
        super(returnType, isVarArg, paramTypes);
        this.name = name;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final EnclosingScopeProvider scope) {
        enclosingScope = scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof NamedFunctionType type) { // @formatter:off
            return name.equals(type.name)
                && (enclosingScope == null || enclosingScope.equals(type.enclosingScope))
                && super.equals(type);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return toString(name.toString());
    }
}

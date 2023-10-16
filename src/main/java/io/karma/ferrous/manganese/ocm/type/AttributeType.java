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

import io.karma.ferrous.manganese.scope.ScopeProvider;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
public final class AttributeType implements Type {
    private final Identifier name;
    private ScopeProvider enclosingScope;

    public AttributeType(final Identifier name) {
        this.name = name;
    }

    @Override
    public long materialize(Target target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public ScopeProvider getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final ScopeProvider scope) {
        enclosingScope = scope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return getInternalName().toString();
    }
}

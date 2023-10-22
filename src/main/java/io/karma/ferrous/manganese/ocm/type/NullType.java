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

import io.karma.ferrous.manganese.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
public final class NullType implements NamedType {
    private Scope enclosingScope;

    // NameProvider

    @Override
    public Identifier getName() {
        return new Identifier(TokenUtils.getLiteral(FerrousLexer.KW_NULL));
    }

    // Scoped

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    // Type

    @Override
    public long materialize(final TargetMachine machine) {
        return 0;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NullType type) {
            return Objects.equals(enclosingScope, type.enclosingScope);
        }
        return false;
    }

    @Override
    public String toString() {
        if (enclosingScope != null) {
            return enclosingScope.toString();
        }
        return super.toString();
    }
}

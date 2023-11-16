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

import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class DummyType implements NamedType {
    public static final DummyType INSTANCE = new DummyType();
    private Scope enclosingScope;

    // @formatter:off
    private DummyType() {}
    // @formatter:on

    @Override
    public Expression makeDefaultValue() {
        throw new UnsupportedOperationException("Dummy type does not have default value");
    }

    @Override
    public TokenSlice getTokenSlice() {
        return TokenSlice.EMPTY;
    }

    @Override
    public GenericParameter[] getGenericParams() {
        return new GenericParameter[0];
    }

    @Override
    public Identifier getName() {
        return Identifier.EMPTY;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        throw new UnsupportedOperationException("Dummy type cannot be materialized");
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
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }
}

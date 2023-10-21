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

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
public final class AliasedType implements Type, TypeCarrier {
    private final Identifier name;
    private final Type backingType;
    private Scope enclosingScope;

    AliasedType(final Identifier name, final Type backingType) {
        this.name = name;
        this.backingType = backingType;
    }

    @Override
    public Type getType() {
        return backingType;
    }

    @Override
    public boolean isAliased() {
        return true;
    }

    @Override
    public Identifier getName() {
        return name; // Override the name only
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return backingType.materialize(machine);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return backingType.getAttributes();
    }

    @Override
    public Type getBaseType() {
        return backingType.getBaseType();
    }

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingScope = scope;
    }
}

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

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
public final class AliasedType implements NamedType {
    private final Identifier name;
    private Type backingType;
    private Scope enclosingScope;

    AliasedType(final Identifier name, final Type backingType) {
        this.name = name;
        this.backingType = backingType;
    }

    public Type getBackingType() {
        return backingType;
    }

    public void setBackingType(final Type type) {
        backingType = type;
    }

    // Name provider

    @Override
    public Identifier getName() {
        return name;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
        enclosingScope = scope;
    }

    // Type

    @Override
    public boolean isAliased() {
        return true;
    }

    @Override
    public boolean isBuiltin() {
        return backingType.isBuiltin();
    }

    @Override
    public boolean isComplete() {
        return backingType.isComplete();
    }

    @Override
    public boolean isReference() {
        return backingType.isReference();
    }

    @Override
    public boolean isPointer() {
        return backingType.isPointer();
    }

    @Override
    public boolean isSlice() {
        return backingType.isSlice();
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
    public int hashCode() {
        return Objects.hash(name, backingType, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof AliasedType type) { // @formatter:off
            return name.equals(type.name)
                && backingType.equals(type.backingType)
                && Objects.equals(enclosingScope, type.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getQualifiedName(), backingType);
    }
}

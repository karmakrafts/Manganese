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
import io.karma.ferrous.manganese.util.Mangler;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class AliasedType implements Type {
    private final Identifier name;
    private final List<GenericParameter> genericParams;
    private final TokenSlice tokenSlice;
    private final HashMap<String, MonomorphizedType> monomorphizationCache = new HashMap<>();
    private Type backingType;
    private Scope enclosingScope;

    AliasedType(final Identifier name, final Type backingType, final TokenSlice tokenSlice,
                final List<GenericParameter> genericParams) {
        this.name = name;
        this.genericParams = genericParams;
        this.tokenSlice = tokenSlice;
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
    public boolean isMonomorphic() {
        return genericParams.isEmpty();
    }

    @Override
    public Type monomorphize(final List<Type> genericTypes) {
        return monomorphizationCache.computeIfAbsent(Mangler.mangleSequence(genericTypes),
            key -> new MonomorphizedType(this, genericTypes));
    }

    @Override
    public boolean canAccept(final TargetMachine targetMachine, final Type type) {
        return backingType.canAccept(targetMachine, type);
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return backingType.makeDefaultValue(targetMachine);
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public List<GenericParameter> getGenericParams() {
        return genericParams;
    }

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
    public boolean isRef() {
        return backingType.isRef();
    }

    @Override
    public boolean isPtr() {
        return backingType.isPtr();
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return backingType.materialize(machine);
    }

    @Override
    public List<TypeAttribute> getAttributes() {
        return backingType.getAttributes();
    }

    @Override
    public Type getBaseType() {
        return backingType.getBaseType();
    }

    // Object

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

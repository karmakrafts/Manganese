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

import io.karma.ferrous.manganese.mangler.Mangler;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 27/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class MonomorphizedType implements Type {
    private final Type baseType;
    private final List<Type> genericTypes;
    private Scope enclosingScope;

    MonomorphizedType(final Type baseType, final List<Type> genericTypes) {
        this.baseType = baseType;
        this.genericTypes = genericTypes;
    }

    @Override
    public String getMangledName() {
        return STR."\{Type.super.getMangledName()}<\{Mangler.mangleSequence(genericTypes)}>";
    }

    @Override
    public Identifier getName() {
        return baseType.getName();
    }

    @Override
    public @Nullable Scope getEnclosingScope() {
        return enclosingScope;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return 0L;
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return null;
    }
}

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
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.Target2LongFunction;
import io.karma.ferrous.manganese.util.TokenUtils;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.system.MemoryUtil;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class BuiltinType implements Type {
    private final Identifier name;
    private final Target2LongFunction typeProvider;
    private long materializedType = MemoryUtil.NULL;

    BuiltinType(final Identifier name, final Target2LongFunction typeProvider) {
        this.name = name;
        this.typeProvider = typeProvider;
    }

    BuiltinType(final int token, final Target2LongFunction typeProvider) {
        this(new Identifier(TokenUtils.getLiteral(token)), typeProvider);
    }

    @Override
    public EnclosingScopeProvider getEnclosingScope() {
        return Scope.GLOBAL;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public long materialize(final Target target) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        return materializedType = typeProvider.getAddress(target);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BuiltinType type) {
            return name.equals(type.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return name.toString();
    }
}

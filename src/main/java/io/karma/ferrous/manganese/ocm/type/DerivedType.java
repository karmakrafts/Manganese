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

import io.karma.ferrous.manganese.ocm.NameProvider;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class DerivedType implements NamedType {
    private final Type baseType;
    private final TypeAttribute[] attributes;
    private final GenericParameter[] genericParams;
    private final TokenSlice tokenSlice;
    private long materializedType = MemoryUtil.NULL;

    DerivedType(final Type baseType, final TypeAttribute... attributes) {
        this.baseType = baseType;
        this.attributes = attributes;

        // Deep-copy generic parameters
        final var params = baseType.getGenericParams();
        final var numParams = params.length;
        genericParams = new GenericParameter[numParams];
        for (var i = 0; i < numParams; i++) {
            final var param = params[i];
            genericParams[i] = new GenericParameter(param.getName(),
                type -> param.getConstraints().test(type),
                param.getValue());
        }

        // Calculate token range
        final var baseSlice = baseType.getTokenSlice();
        if (baseSlice != TokenSlice.EMPTY) {
            var begin = baseSlice.begin();
            var end = baseSlice.end();
            for (final var attrib : attributes) {
                begin -= attrib.getLHWidth();
                end += attrib.getRHWidth();
            }
            tokenSlice = new TokenSlice(baseSlice.tokenStream(), begin, end);
        }
        else {
            tokenSlice = TokenSlice.EMPTY;
        }
    }

    // NameProvider

    @Override
    public Identifier getName() {
        if (baseType instanceof NameProvider provider) {
            return provider.getName();
        }
        return Identifier.EMPTY;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return baseType.getEnclosingScope();
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    // Type

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public GenericParameter[] getGenericParams() {
        return genericParams;
    }

    @Override
    public Type getBaseType() {
        return baseType;
    }

    @Override
    public long materialize(final TargetMachine machine) {
        if (materializedType != MemoryUtil.NULL) {
            return materializedType;
        }
        materializedType = baseType.materialize(machine);
        for (var i = 0; i < attributes.length; i++) {
            materializedType = LLVMCore.LLVMPointerType(materializedType, 0);
        }
        return materializedType;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return attributes;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(baseType, Arrays.hashCode(attributes));
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DerivedType type) { // @formatter:off
            return baseType.equals(type.baseType)
                && Arrays.equals(attributes, type.attributes);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        var result = baseType.toString();
        for (final var attrib : attributes) {
            result = attrib.format(result);
        }
        return result;
    }
}

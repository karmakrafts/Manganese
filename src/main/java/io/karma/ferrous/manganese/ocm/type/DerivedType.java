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
import io.karma.ferrous.manganese.ocm.constant.NullConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TypeMod;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.util.EnumSet;
import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public final class DerivedType implements NamedType {
    private final Type baseType;
    private final @Nullable TypeAttribute attribute;
    private final GenericParameter[] genericParams;
    private final TokenSlice tokenSlice;
    private final EnumSet<TypeMod> modifiers;
    private long materializedType = MemoryUtil.NULL;

    DerivedType(final Type baseType, final @Nullable TypeAttribute attribute, final EnumSet<TypeMod> modifiers) {
        this.baseType = baseType;
        this.attribute = attribute;
        this.modifiers = modifiers;
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
        tokenSlice = baseType.getTokenSlice();
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
    public EnumSet<TypeMod> getModifiers() {
        return modifiers;
    }

    @Override
    public boolean canAccept(final Type type) {
        if (type instanceof DerivedType derivedType) {
            return baseType == derivedType.baseType && attribute == derivedType.attribute;
        }
        return NamedType.super.canAccept(type);
    }

    @Override
    public Expression makeDefaultValue() {
        if (isPointer()) {
            final var value = new NullConstant(TokenSlice.EMPTY);
            value.setContextualType(baseType);
            return value;
        }
        return baseType.makeDefaultValue();
    }

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
        return materializedType = LLVMCore.LLVMPointerType(baseType.materialize(machine), 0);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        final var baseAttribs = baseType.getAttributes();
        if (attribute == null) {
            return baseAttribs;
        }
        final var numBaseAttribs = baseAttribs.length;
        final var attribs = new TypeAttribute[numBaseAttribs + 1];
        System.arraycopy(baseAttribs, 0, attribs, 0, numBaseAttribs);
        attribs[numBaseAttribs] = attribute;
        return attribs;
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(baseType, attribute);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof DerivedType type) { // @formatter:off
            return baseType.equals(type.baseType)
                && attribute == type.attribute;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        if (attribute == null) {
            return baseType.toString();
        }
        return attribute.format(baseType.toString());
    }
}

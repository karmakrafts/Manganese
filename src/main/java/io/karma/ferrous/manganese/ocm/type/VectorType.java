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

import io.karma.ferrous.manganese.ocm.expr.AllocExpression;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;

import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 21/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class VectorType implements Type {
    private final Type type;
    private final int elementCount;
    private final TokenSlice tokenSlice;
    private final GenericParameter[] genericParams;
    private long materializedType = NULL;
    private Scope enclosingScope;

    public VectorType(final Type type, final int elementCount, final TokenSlice tokenSlice,
                      final GenericParameter... genericParams) {
        this.type = type;
        this.elementCount = elementCount;
        this.tokenSlice = tokenSlice;
        this.genericParams = genericParams;
    }

    public Type getType() {
        return type;
    }

    public int getElementCount() {
        return elementCount;
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
    public Expression makeDefaultValue() {
        return new AllocExpression(this, false, TokenSlice.EMPTY);
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
    public long materialize(final TargetMachine machine) {
        if (materializedType == NULL) {
            materializedType = LLVMCore.LLVMVectorType(type.materialize(machine), elementCount);
        }
        return materializedType;
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
        return Objects.hash(type, elementCount, enclosingScope);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof VectorType vecType) { // @formatter:off
            return type.equals(vecType.type)
                && elementCount == vecType.elementCount
                && Objects.equals(enclosingScope, vecType.enclosingScope);
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        final var buffer = new StringBuilder();
        buffer.append('(');
        for (var i = 0; i < elementCount; i++) {
            buffer.append(type);
            if (i < elementCount - 1) {
                buffer.append(", ");
            }
        }
        buffer.append(')');
        return buffer.toString();
    }
}

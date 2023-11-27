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

package io.karma.ferrous.manganese.ocm.expr;

import io.karma.ferrous.manganese.ocm.Parameter;
import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.field.Field;
import io.karma.ferrous.manganese.ocm.function.FunctionReference;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.ocm.statement.LetStatement;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.ocm.type.TypeAttribute;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.TokenSlice;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Alexander Hinze
 * @since 08/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ReferenceExpression implements Expression {
    private final Object reference;
    private final TokenSlice tokenSlice;
    private Scope enclosingScope;
    private boolean isWrite;

    /**
     * @param reference  The reference to a {@link FunctionReference}, {@link Field} or {@link LetStatement}.
     * @param tokenSlice The token slice which defines this reference.
     */
    public ReferenceExpression(final Object reference, final boolean isWrite, final TokenSlice tokenSlice) {
        this.reference = reference;
        this.isWrite = isWrite;
        this.tokenSlice = tokenSlice;
    }

    public Object getReference() {
        return reference;
    }

    public void setIsWrite(final boolean isWrite) {
        this.isWrite = isWrite;
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

    // Expression

    @Override
    public Type getType() {
        return switch (reference) { // @formatter:off
            case FunctionReference funRef -> Objects.requireNonNull(funRef.get()).getType();
            case ValueStorage storage     -> Objects.requireNonNull(storage.getType());
            case Parameter param          -> param.getType();
            default                       -> throw new IllegalStateException("Unknown reference kind");
        }; // @formatter:on
    }

    @Override
    public TokenSlice getTokenSlice() {
        return tokenSlice;
    }

    @Override
    public long emit(final TargetMachine targetMachine, final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        return switch (reference) {
            case FunctionReference funRef -> {
                final var function = Objects.requireNonNull(funRef.get());
                final var typeAddress = getType().derive(TypeAttribute.POINTER).materialize(targetMachine);
                final var fnAddress = function.materialize(irContext.getModule(), targetMachine);
                yield builder.intToPtr(typeAddress, fnAddress);
            }
            //case ValueStorage storage -> FIXME: FIXMEEEEEEEEEEEEEEE
            //    isWrite ? storage.getAddress(targetMachine, irContext) : storage.load(targetMachine, irContext);
            case Parameter param -> irContext.getParameter(param.getName());
            default -> throw new IllegalStateException("Unknown reference kind");
        };
    }

    // Object

    @Override
    public int hashCode() {
        return Objects.hash(reference, isWrite);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ReferenceExpression refExpr) {
            return reference == refExpr.reference && isWrite == refExpr.isWrite;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", reference, isWrite);
    }
}

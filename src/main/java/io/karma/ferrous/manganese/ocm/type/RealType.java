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

import io.karma.ferrous.manganese.ocm.Mangleable;
import io.karma.ferrous.manganese.ocm.constant.BigRealConstant;
import io.karma.ferrous.manganese.ocm.constant.RealConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;

import java.math.BigDecimal;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public enum RealType implements Type, Mangleable {
    // @formatter:off
    F16 (16,  FerrousLexer.KW_F16,  "H", LLVMCore::LLVMHalfType,   RealType::makeDefaultHalfValue),
    F32 (32,  FerrousLexer.KW_F32,  "F", LLVMCore::LLVMFloatType,  RealType::makeDefaultSingleValue),
    F64 (64,  FerrousLexer.KW_F64,  "D", LLVMCore::LLVMDoubleType, RealType::makeDefaultDoubleValue),
    F128(128, FerrousLexer.KW_F128, "Q", LLVMCore::LLVMFP128Type,  RealType::makeDefaultQuadValue);
    // @formatter:on

    public static final char SEQUENCE_PREFIX = '\'';

    private final int width;
    private final Identifier name;
    private final String mangledName;
    private final LongSupplier factory;
    private final Supplier<Expression> defaultSupplier;

    RealType(final int width, final int token, final String mangledName, final LongSupplier factory,
             final Supplier<Expression> defaultSupplier) {
        this.width = width;
        name = new Identifier(TokenUtils.getLiteral(token));
        this.mangledName = mangledName;
        this.factory = factory;
        this.defaultSupplier = defaultSupplier;
    }

    private static Expression makeDefaultHalfValue() {
        return new RealConstant(F16, 0.0, TokenSlice.EMPTY);
    }

    private static Expression makeDefaultSingleValue() {
        return new RealConstant(F32, 0.0, TokenSlice.EMPTY);
    }

    private static Expression makeDefaultDoubleValue() {
        return new RealConstant(F64, 0.0, TokenSlice.EMPTY);
    }

    private static Expression makeDefaultQuadValue() {
        return new BigRealConstant(F128, BigDecimal.ZERO, TokenSlice.EMPTY);
    }

    public int getWidth() {
        return width;
    }

    public long sizeCast(final RealType type, final long value, final TargetMachine targetMachine,
                         final IRContext irContext) {
        final var builder = irContext.getCurrentOrCreate();
        final var typeAddress = type.materialize(targetMachine);
        final var otherWidth = type.getWidth();
        if (otherWidth == width) {
            return value; // If the width is the same, we don't do anything to it
        }
        if (otherWidth < width) {
            return builder.floatTrunc(typeAddress, value);
        }
        return builder.floatExt(typeAddress, value);
    }

    @Override
    public boolean canAccept(final TargetMachine targetMachine, final Type type) {
        final var kind = type.getKind();
        return kind == TypeKind.REAL || kind == TypeKind.INT;
    }

    @Override
    public char getMangledSequencePrefix() {
        return SEQUENCE_PREFIX;
    }

    @Override
    public String getMangledName() {
        return mangledName;
    }

    @Override
    public boolean isBuiltin() {
        return true;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.REAL;
    }

    @Override
    public Identifier getName() {
        return name;
    }

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope enclosingScope) {
    }

    @Override
    public long materialize(final TargetMachine machine) {
        return factory.getAsLong();
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        return defaultSupplier.get();
    }

    @Override
    public String toString() {
        return getName().toString();
    }
}

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

import io.karma.ferrous.manganese.ocm.constant.*;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.generic.GenericParameter;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

import java.util.EnumSet;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public enum BuiltinType implements NamedType {
    // @formatter:off
    VOID    (FerrousLexer.KW_VOID,  target -> LLVMCore.LLVMVoidType(),   VoidConstant::new),
    BOOL    (FerrousLexer.KW_BOOL,  target -> LLVMCore.LLVMInt8Type(),   () -> new BoolConstant(false, TokenSlice.EMPTY)),
    CHAR    (FerrousLexer.KW_CHAR,  target -> LLVMCore.LLVMInt8Type(),   () -> new CharConstant(' ', TokenSlice.EMPTY)),
    // Signed types
    I8      (FerrousLexer.KW_I8,    target -> LLVMCore.LLVMInt8Type(),   () -> makeDefaultInt(1, false)),
    I16     (FerrousLexer.KW_I16,   target -> LLVMCore.LLVMInt16Type(),  () -> makeDefaultInt(2, false)),
    I32     (FerrousLexer.KW_I32,   target -> LLVMCore.LLVMInt32Type(),  () -> makeDefaultInt(4, false)),
    I64     (FerrousLexer.KW_I64,   target -> LLVMCore.LLVMInt64Type(),  () -> makeDefaultInt(8, false)),
    ISIZE   (FerrousLexer.KW_ISIZE, BuiltinType::getSizedIntType,        () -> makeDefaultInt(-1, false)),
    // Unsigned types
    U8      (FerrousLexer.KW_U8,    target -> LLVMCore.LLVMInt8Type(),   () -> makeDefaultInt(1, true)),
    U16     (FerrousLexer.KW_U16,   target -> LLVMCore.LLVMInt16Type(),  () -> makeDefaultInt(2, true)),
    U32     (FerrousLexer.KW_U32,   target -> LLVMCore.LLVMInt32Type(),  () -> makeDefaultInt(4, true)),
    U64     (FerrousLexer.KW_U64,   target -> LLVMCore.LLVMInt64Type(),  () -> makeDefaultInt(8, true)),
    USIZE   (FerrousLexer.KW_USIZE, BuiltinType::getSizedIntType,        () -> makeDefaultInt(-1, true)),
    // Floating point types
    F32     (FerrousLexer.KW_F32,   target -> LLVMCore.LLVMFloatType(),  () -> makeDefaultReal(4)),
    F64     (FerrousLexer.KW_F64,   target -> LLVMCore.LLVMDoubleType(), () -> makeDefaultReal(8));
    // @formatter:on

    public static final EnumSet<BuiltinType> SIGNED_INT_TYPES = EnumSet.of(I8, I16, I32, I64, ISIZE);
    public static final EnumSet<BuiltinType> UNSIGNED_INT_TYPES = EnumSet.of(U8, U16, U32, U64, USIZE);
    public static final EnumSet<BuiltinType> FLOAT_TYPES = EnumSet.of(F32, F64);
    private final Identifier name;
    private final ToLongFunction<TargetMachine> typeProvider;
    private final Supplier<Expression> defaultProvider;

    BuiltinType(final Identifier name, final ToLongFunction<TargetMachine> typeProvider,
                final Supplier<Expression> defaultProvider) {
        this.name = name;
        this.typeProvider = typeProvider;
        this.defaultProvider = defaultProvider;
    }

    BuiltinType(final int token, final ToLongFunction<TargetMachine> typeProvider,
                final Supplier<Expression> defaultProvider) {
        this(new Identifier(TokenUtils.getLiteral(token)), typeProvider, defaultProvider);
    }

    private static Expression makeDefaultInt(final int size, final boolean isUnsigned) {
        return switch(size) { // @formatter:off
            case 1  -> new IntConstant(isUnsigned ? U8 : I8, 0, TokenSlice.EMPTY);
            case 2  -> new IntConstant(isUnsigned ? U16 : I16, 0, TokenSlice.EMPTY);
            case 4  -> new IntConstant(isUnsigned ? U32 : I32, 0, TokenSlice.EMPTY);
            case 8  -> new IntConstant(isUnsigned ? U64 : I64, 0, TokenSlice.EMPTY);
            case -1 -> new IntConstant(isUnsigned ? USIZE : ISIZE, 0, TokenSlice.EMPTY);
            default -> throw new IllegalArgumentException("");
        }; // @formatter:on
    }

    private static Expression makeDefaultReal(final int size) {
        return switch(size) { // @formatter:off
            case 4  -> new RealConstant(F32, 0.0, TokenSlice.EMPTY);
            case 8  -> new RealConstant(F64, 0.0, TokenSlice.EMPTY);
            default -> throw new IllegalArgumentException("");
        }; // @formatter:on
    }

    private static long getSizedIntType(final TargetMachine machine) {
        return machine.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
    }

    public boolean isSignedInt() {
        return SIGNED_INT_TYPES.contains(this);
    }

    public boolean isUnsignedInt() {
        return UNSIGNED_INT_TYPES.contains(this);
    }

    public boolean isInt() {
        return isSignedInt() || isUnsignedInt();
    }

    public boolean isFloatType() {
        return FLOAT_TYPES.contains(this);
    }

    // NameProvider

    @Override
    public Identifier getName() {
        return name;
    }

    // Scoped

    @Override
    public Scope getEnclosingScope() {
        return DefaultScope.GLOBAL;
    }

    @Override
    public void setEnclosingScope(final Scope scope) {
    }

    // Type

    @Override
    public Expression makeDefaultValue() {
        return defaultProvider.get();
    }

    @Override
    public TokenSlice getTokenSlice() {
        return TokenSlice.EMPTY;
    }

    @Override
    public GenericParameter[] getGenericParams() {
        return new GenericParameter[0];
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
    public long materialize(final TargetMachine machine) {
        return typeProvider.applyAsLong(machine);
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return new TypeAttribute[0];
    }

    // Object

    @Override
    public String toString() {
        return name.toString();
    }
}

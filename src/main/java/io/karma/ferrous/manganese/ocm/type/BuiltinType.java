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

import java.util.function.ToLongFunction;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public enum BuiltinType implements NamedType {
    // @formatter:off
    VOID    (FerrousLexer.KW_VOID,  target -> LLVMCore.LLVMVoidType()),
    BOOL    (FerrousLexer.KW_BOOL,  target -> LLVMCore.LLVMInt8Type()),
    CHAR    (FerrousLexer.KW_CHAR,  target -> LLVMCore.LLVMInt8Type()),
    // Signed types
    I8      (FerrousLexer.KW_I8,    target -> LLVMCore.LLVMInt8Type()),
    I16     (FerrousLexer.KW_I16,   target -> LLVMCore.LLVMInt16Type()),
    I32     (FerrousLexer.KW_I32,   target -> LLVMCore.LLVMInt32Type()),
    I64     (FerrousLexer.KW_I64,   target -> LLVMCore.LLVMInt64Type()),
    ISIZE   (FerrousLexer.KW_ISIZE, BuiltinType::getSizedIntType),
    // Unsigned types
    U8      (FerrousLexer.KW_U8,    target -> LLVMCore.LLVMInt8Type()),
    U16     (FerrousLexer.KW_U16,   target -> LLVMCore.LLVMInt16Type()),
    U32     (FerrousLexer.KW_U32,   target -> LLVMCore.LLVMInt32Type()),
    U64     (FerrousLexer.KW_U64,   target -> LLVMCore.LLVMInt64Type()),
    USIZE   (FerrousLexer.KW_USIZE, BuiltinType::getSizedIntType),
    // Floating point types
    F32     (FerrousLexer.KW_F32,   target -> LLVMCore.LLVMFloatType()),
    F64     (FerrousLexer.KW_F64,   target -> LLVMCore.LLVMDoubleType());
    // @formatter:on

    private final Identifier name;
    private final ToLongFunction<TargetMachine> typeProvider;

    BuiltinType(final Identifier name, final ToLongFunction<TargetMachine> typeProvider) {
        this.name = name;
        this.typeProvider = typeProvider;
    }

    BuiltinType(final int token, final ToLongFunction<TargetMachine> typeProvider) {
        this(new Identifier(TokenUtils.getLiteral(token)), typeProvider);
    }

    private static long getSizedIntType(final TargetMachine machine) {
        return machine.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
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
    public TokenSlice getTokenSlice() {
        return null;
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

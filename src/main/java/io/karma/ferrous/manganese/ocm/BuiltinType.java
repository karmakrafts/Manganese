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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Target2LongFunction;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public record BuiltinType(String name, Target2LongFunction baseTypeProvider,
                          TypeAttribute... attributes) implements Type {
    public static final BuiltinType VOID = new BuiltinType(FerrousLexer.KW_VOID, target -> LLVMCore.LLVMVoidType());
    public static final BuiltinType BOOL = new BuiltinType(FerrousLexer.KW_BOOL, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType CHAR = new BuiltinType(FerrousLexer.KW_CHAR, target -> LLVMCore.LLVMInt8Type());
    // Signed types
    public static final BuiltinType I8 = new BuiltinType(FerrousLexer.KW_I8, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType I16 = new BuiltinType(FerrousLexer.KW_I16, target -> LLVMCore.LLVMInt16Type());
    public static final BuiltinType I32 = new BuiltinType(FerrousLexer.KW_I32, target -> LLVMCore.LLVMInt32Type());
    public static final BuiltinType I64 = new BuiltinType(FerrousLexer.KW_I64, target -> LLVMCore.LLVMInt64Type());
    public static final BuiltinType ISIZE = new BuiltinType(FerrousLexer.KW_ISIZE, BuiltinType::getSizedIntType);
    public static final BuiltinType[] SIGNED_TYPES = {I8, I16, I32, I64, ISIZE};
    // Unsigned types
    public static final BuiltinType U8 = new BuiltinType(FerrousLexer.KW_U8, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType U16 = new BuiltinType(FerrousLexer.KW_U16, target -> LLVMCore.LLVMInt16Type());
    public static final BuiltinType U32 = new BuiltinType(FerrousLexer.KW_U32, target -> LLVMCore.LLVMInt32Type());
    public static final BuiltinType U64 = new BuiltinType(FerrousLexer.KW_U64, target -> LLVMCore.LLVMInt64Type());
    public static final BuiltinType USIZE = new BuiltinType(FerrousLexer.KW_USIZE, BuiltinType::getSizedIntType);
    public static final BuiltinType[] UNSIGNED_TYPES = {U8, U16, U32, U64, USIZE};
    // Floating point types
    public static final BuiltinType F32 = new BuiltinType(FerrousLexer.KW_F32, target -> LLVMCore.LLVMFloatType());
    public static final BuiltinType F64 = new BuiltinType(FerrousLexer.KW_F64, target -> LLVMCore.LLVMDoubleType());
    public static final BuiltinType[] FLOAT_TYPES = {F32, F64};

    public static final BuiltinType[] BUILTIN_TYPES = { // @formatter:off
        VOID, BOOL, CHAR,
        I8, I16, I32, I64, ISIZE,
        U8, U16, U32, U64, USIZE,
        F32, F64
    }; // @formatter:on

    public BuiltinType(final int token, final Target2LongFunction baseTypeProvider, final TypeAttribute... attributes) {
        this(TokenUtils.getLiteral(token), baseTypeProvider, attributes);
    }

    public static Optional<BuiltinType> findBuiltinType(final String name) {
        return Arrays.stream(BUILTIN_TYPES).filter(type -> type.name.equals(name)).findFirst();
    }

    private static long getSizedIntType(final Target target) {
        return target.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
    }

    @Override
    public long materialize(final Target target) {
        var result = baseTypeProvider.getAddress(target);
        final var numAttribs = attributes.length;
        for (var i = 0; i < numAttribs; i++) {
            result = LLVMCore.LLVMPointerType(result, 0);
        }
        return result;
    }

    @Override
    public TypeAttribute[] getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        var result = name;
        for (final var attrib : attributes) {
            result = attrib.format(result);
        }
        return result;
    }
}

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

import io.karma.ferrous.manganese.CompileError;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.translate.TranslationException;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
public final class Types {
    public static final BuiltinType VOID = new BuiltinType(FerrousLexer.KW_VOID, target -> LLVMCore.LLVMVoidType());
    public static final BuiltinType BOOL = new BuiltinType(FerrousLexer.KW_BOOL, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType CHAR = new BuiltinType(FerrousLexer.KW_CHAR, target -> LLVMCore.LLVMInt8Type());
    // Signed types
    public static final BuiltinType I8 = new BuiltinType(FerrousLexer.KW_I8, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType I16 = new BuiltinType(FerrousLexer.KW_I16, target -> LLVMCore.LLVMInt16Type());
    public static final BuiltinType I32 = new BuiltinType(FerrousLexer.KW_I32, target -> LLVMCore.LLVMInt32Type());
    public static final BuiltinType I64 = new BuiltinType(FerrousLexer.KW_I64, target -> LLVMCore.LLVMInt64Type());
    public static final BuiltinType ISIZE = new BuiltinType(FerrousLexer.KW_ISIZE, Types::getSizedIntType);
    public static final BuiltinType[] SIGNED_TYPES = {I8, I16, I32, I64, ISIZE};
    // Unsigned types
    public static final BuiltinType U8 = new BuiltinType(FerrousLexer.KW_U8, target -> LLVMCore.LLVMInt8Type());
    public static final BuiltinType U16 = new BuiltinType(FerrousLexer.KW_U16, target -> LLVMCore.LLVMInt16Type());
    public static final BuiltinType U32 = new BuiltinType(FerrousLexer.KW_U32, target -> LLVMCore.LLVMInt32Type());
    public static final BuiltinType U64 = new BuiltinType(FerrousLexer.KW_U64, target -> LLVMCore.LLVMInt64Type());
    public static final BuiltinType USIZE = new BuiltinType(FerrousLexer.KW_USIZE, Types::getSizedIntType);
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

    private static final HashMap<String, Type> CACHE = new HashMap<>();

    // @formatter:off
    private Types() {}
    // @formatter:on

    public static void invalidateCache() {
        CACHE.clear();
    }

    private static long getSizedIntType(final Target target) {
        return target.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
    }

    @SuppressWarnings("unchecked")
    static <T extends Type> T cached(final T type) {
        final var key = type.toString();
        final var result = CACHE.get(key);
        if (result != null) {
            return (T) result;
        }
        CACHE.put(key, type);
        return type;
    }

    public static BuiltinType builtin(final Identifier name) { // @formatter:off
        return Arrays.stream(BUILTIN_TYPES)
            .filter(type -> type.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new TranslationException(new CompileError(String.format("Unknown builtin '%s'", name))));
    } // @formatter:on

    public static FunctionType function(final Type returnType, final List<? extends Type> paramTypes,
                                        final boolean isVarArg) {
        return cached(new FunctionType(returnType, isVarArg, paramTypes.toArray(Type[]::new)));
    }

    public static FunctionType function(final Type returnType, final boolean isVarArg, final Type... paramTypes) {
        return cached(new FunctionType(returnType, isVarArg, paramTypes));
    }

    public static NamedFunctionType namedFunction(final Identifier name, final Type returnType,
                                                  final List<? extends Type> paramTypes, final boolean isVarArg) {
        return cached(new NamedFunctionType(name, returnType, isVarArg, paramTypes.toArray(Type[]::new)));
    }

    public static NamedFunctionType namedFunction(final Identifier name, final Type returnType, final boolean isVarArg,
                                                  final Type... paramTypes) {
        return cached(new NamedFunctionType(name, returnType, isVarArg, paramTypes));
    }

    public static StructureType structure(final Identifier name, final boolean isPacked, final Type... fieldTypes) {
        return cached(new StructureType(name, isPacked, fieldTypes));
    }

    public static StructureType structure(final Identifier name, final Type... fieldTypes) {
        return structure(name, false, fieldTypes);
    }

    public static IncompleteType incomplete(final Identifier name) {
        return cached(new IncompleteType(name));
    }
}

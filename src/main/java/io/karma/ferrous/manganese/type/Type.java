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

package io.karma.ferrous.manganese.type;

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.translate.TypeTranslationUnit;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Target2LongFunction;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class Type {
    public static final Type VOID = new Type("void", target -> LLVMCore.LLVMVoidType());
    public static final Type BOOL = new Type("bool", target -> LLVMCore.LLVMInt8Type());
    public static final Type CHAR = new Type("char", target -> LLVMCore.LLVMInt8Type());
    // Signed types
    public static final Type I8 = new Type("i8", target -> LLVMCore.LLVMInt8Type());
    public static final Type I16 = new Type("i16", target -> LLVMCore.LLVMInt16Type());
    public static final Type I32 = new Type("i32", target -> LLVMCore.LLVMInt32Type());
    public static final Type I64 = new Type("i64", target -> LLVMCore.LLVMInt64Type());
    public static final Type ISIZE = new Type("isize", Type::getSizedIntType);
    public static final Type[] SIGNED_TYPES = {I8, I16, I32, I64, ISIZE};
    // Unsigned types
    public static final Type U8 = new Type("u8", target -> LLVMCore.LLVMInt8Type());
    public static final Type U16 = new Type("u16", target -> LLVMCore.LLVMInt16Type());
    public static final Type U32 = new Type("u32", target -> LLVMCore.LLVMInt32Type());
    public static final Type U64 = new Type("u64", target -> LLVMCore.LLVMInt64Type());
    public static final Type USIZE = new Type("usize", Type::getSizedIntType);
    public static final Type[] UNSIGNED_TYPES = {U8, U16, U32, U64, USIZE};
    // Floating point types
    public static final Type F32 = new Type("f32", target -> LLVMCore.LLVMFloatType());
    public static final Type F64 = new Type("f64", target -> LLVMCore.LLVMDoubleType());
    public static final Type[] FLOAT_TYPES = {F32, F64};

    public static final Type[] BUILTIN_TYPES = { // @formatter:off
        VOID, BOOL, CHAR,
        I8, I16, I32, I64, ISIZE,
        U8, U16, U32, U64, USIZE,
        F32, F64
    }; // @formatter:on

    private static final HashMap<String, Type> NAMED_TYPE_CACHE = new HashMap<>();

    private final String baseName;
    private final Target2LongFunction baseTypeProvider;
    private final int sliceDepth;
    private final int pointerDepth;
    private final boolean isReference;

    public Type(final String baseName, final Target2LongFunction baseTypeProvider, final int pointerDepth,
                final int sliceDepth, final boolean isReference) {
        this.baseName = baseName;
        this.baseTypeProvider = baseTypeProvider;
        this.pointerDepth = pointerDepth;
        this.sliceDepth = sliceDepth;
        this.isReference = isReference;
    }

    public Type(final String baseName, final Target2LongFunction baseTypeProvider) {
        this(baseName, baseTypeProvider, 0, 0, false);
    }

    public static Optional<Type> findBuiltinType(final String name) {
        return Arrays.stream(BUILTIN_TYPES).filter(type -> type.baseName.equals(name)).findFirst();
    }

    public static Optional<Type> findType(final Compiler compiler, final TypeContext context) {
        final TypeTranslationUnit unit = new TypeTranslationUnit(compiler);
        ParseTreeWalker.DEFAULT.walk(unit, context);
        if (!compiler.getStatus().isRecoverable()) {
            return Optional.empty();
        }
        return Optional.of(unit.materializeType());
    }

    private static long getSizedIntType(final Target target) {
        return target.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
    }

    public Type derive(final int sliceDepth, final int pointerDepth, final boolean isReference) {
        return new Type(baseName, baseTypeProvider, sliceDepth, pointerDepth, isReference);
    }

    public Type derivePointer(final int depth) {
        return derive(0, depth, false);
    }

    public Type deriveSlice(final int depth) {
        return derive(depth, 0, false);
    }

    public Type deriveReference() {
        return derive(0, 0, true);
    }

    public long materialize(final Target target) {
        var result = baseTypeProvider.getAddress(target);
        for (var i = 0; i < pointerDepth; i++) {
            result = LLVMCore.LLVMPointerType(result, 0);
        }
        if (isReference) {
            result = LLVMCore.LLVMPointerType(result, 0); // One pointer for refs
        }
        return result;
    }

    public String getBaseName() {
        return baseName;
    }

    public int getPointerDepth() {
        return pointerDepth;
    }

    public boolean isReference() {
        return isReference;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseName, pointerDepth, isReference);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Type type) {
            return baseName.equals(type.baseName) && pointerDepth == type.pointerDepth && isReference == type.isReference;
        }
        return false;
    }

    @Override
    public String toString() {
        final var builder = new StringBuilder();
        if (isReference) {
            builder.append('&');
        }
        builder.append("*".repeat(pointerDepth));
        builder.append(baseName);
        return builder.toString();
    }
}

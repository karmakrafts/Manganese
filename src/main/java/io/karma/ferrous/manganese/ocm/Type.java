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

import io.karma.ferrous.manganese.Compiler;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.translate.TypeTranslationUnit;
import io.karma.ferrous.manganese.util.Target2LongFunction;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser.TypeContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public record Type(String baseName, Target2LongFunction baseTypeProvider, TypeAttribute... attributes) {
    public static final Type VOID = new Type(FerrousLexer.KW_VOID, target -> LLVMCore.LLVMVoidType());
    public static final Type BOOL = new Type(FerrousLexer.KW_BOOL, target -> LLVMCore.LLVMInt8Type());
    public static final Type CHAR = new Type(FerrousLexer.KW_CHAR, target -> LLVMCore.LLVMInt8Type());
    // Signed types
    public static final Type I8 = new Type(FerrousLexer.KW_I8, target -> LLVMCore.LLVMInt8Type());
    public static final Type I16 = new Type(FerrousLexer.KW_I16, target -> LLVMCore.LLVMInt16Type());
    public static final Type I32 = new Type(FerrousLexer.KW_I32, target -> LLVMCore.LLVMInt32Type());
    public static final Type I64 = new Type(FerrousLexer.KW_I64, target -> LLVMCore.LLVMInt64Type());
    public static final Type ISIZE = new Type(FerrousLexer.KW_ISIZE, Type::getSizedIntType);
    public static final Type[] SIGNED_TYPES = {I8, I16, I32, I64, ISIZE};
    // Unsigned types
    public static final Type U8 = new Type(FerrousLexer.KW_U8, target -> LLVMCore.LLVMInt8Type());
    public static final Type U16 = new Type(FerrousLexer.KW_U16, target -> LLVMCore.LLVMInt16Type());
    public static final Type U32 = new Type(FerrousLexer.KW_U32, target -> LLVMCore.LLVMInt32Type());
    public static final Type U64 = new Type(FerrousLexer.KW_U64, target -> LLVMCore.LLVMInt64Type());
    public static final Type USIZE = new Type(FerrousLexer.KW_USIZE, Type::getSizedIntType);
    public static final Type[] UNSIGNED_TYPES = {U8, U16, U32, U64, USIZE};
    // Floating point types
    public static final Type F32 = new Type(FerrousLexer.KW_F32, target -> LLVMCore.LLVMFloatType());
    public static final Type F64 = new Type(FerrousLexer.KW_F64, target -> LLVMCore.LLVMDoubleType());
    public static final Type[] FLOAT_TYPES = {F32, F64};

    public static final Type[] BUILTIN_TYPES = { // @formatter:off
        VOID, BOOL, CHAR,
        I8, I16, I32, I64, ISIZE,
        U8, U16, U32, U64, USIZE,
        F32, F64
    }; // @formatter:on

    public Type(final int token, final Target2LongFunction baseTypeProvider, final TypeAttribute... attributes) {
        this(TokenUtils.getLiteral(token), baseTypeProvider, attributes);
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
        return Optional.of(unit.getType());
    }

    private static long getSizedIntType(final Target target) {
        return target.getPointerSize() == 8 ? LLVMCore.LLVMInt64Type() : LLVMCore.LLVMInt32Type();
    }

    public Type derive(final TypeAttribute... attributes) {
        return new Type(baseName, baseTypeProvider, attributes);
    }

    public Type derivePointer(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.POINTER);
        return derive(attribs);
    }

    public Type deriveSlice(final int depth) {
        final var attribs = new TypeAttribute[depth];
        Arrays.fill(attribs, TypeAttribute.SLICE);
        return derive(attribs);
    }

    public Type deriveReference() {
        return derive(TypeAttribute.REFERENCE);
    }

    public long materialize(final Target target) {
        var result = baseTypeProvider.getAddress(target);
        final var numAttribs = attributes.length;
        for (var i = 0; i < numAttribs; i++) {
            result = LLVMCore.LLVMPointerType(result, 0);
        }
        return result;
    }

    public String getBaseName() {
        return baseName;
    }

    public TypeAttribute[] getAttributes() {
        return attributes;
    }

    public boolean isReference() {
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.REFERENCE;
    }

    public boolean isPointer() {
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.POINTER;
    }

    public boolean isSlice() {
        if (attributes.length == 0) {
            return false;
        }
        return attributes[attributes.length - 1] == TypeAttribute.SLICE;
    }

    @Override
    public String toString() {
        var result = baseName;
        for (final var attrib : attributes) {
            result = attrib.format(result);
        }
        return result;
    }
}

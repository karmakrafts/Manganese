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
import io.karma.ferrous.manganese.ocm.constant.BigIntConstant;
import io.karma.ferrous.manganese.ocm.constant.IntConstant;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.scope.DefaultScope;
import io.karma.ferrous.manganese.ocm.scope.Scope;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import io.karma.ferrous.manganese.util.TokenSlice;
import io.karma.ferrous.manganese.util.TokenUtils;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryUtil;

import java.math.BigInteger;

/**
 * @author Alexander Hinze
 * @since 03/12/2023
 */
@API(status = API.Status.INTERNAL)
public enum SizeType implements Type, Mangleable {
    // @formatter:off
    ISIZE(FerrousLexer.KW_ISIZE, "SZ", false),
    USIZE(FerrousLexer.KW_USIZE, "UZ", true);
    // @formatter:on

    public static final char SEQUENCE_PREFIX = '\'';

    private final boolean isUnsigned;
    private final Identifier name;
    private final String mangledName;
    private long materializedType;

    SizeType(final int token, final String mangledName, final boolean isUnsigned) {
        name = new Identifier(TokenUtils.getLiteral(token));
        this.mangledName = mangledName;
        this.isUnsigned = isUnsigned;
    }

    public boolean isUnsigned() {
        return isUnsigned;
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
        if (materializedType == MemoryUtil.NULL) {
            materializedType = LLVMCore.LLVMIntType(machine.getPointerSize() << 3);
        }
        return materializedType;
    }

    @Override
    public Type getBaseType() {
        return this;
    }

    @Override
    public Expression makeDefaultValue(final TargetMachine targetMachine) {
        if (targetMachine.getPointerSize() <= 8) {
            return new IntConstant(this, 0, TokenSlice.EMPTY);
        }
        return new BigIntConstant(this, BigInteger.ZERO, TokenSlice.EMPTY);
    }

    @Override
    public String toString() {
        return getName().toString();
    }
}

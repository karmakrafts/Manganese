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

package io.karma.ferrous.manganese.util;

import org.lwjgl.llvm.LLVMCore;

/**
 * @author Cedric Hammes, Cach30verfl0w
 * @since 23/12/2023
 */
public enum EnumCompareOp {

    GREATER_THAN(LLVMCore.LLVMIntSGT, LLVMCore.LLVMIntUGT),
    GREATER_THAN_EQUAL(LLVMCore.LLVMIntSGE, LLVMCore.LLVMIntUGE),
    LOWER_THAN(LLVMCore.LLVMIntSLT, LLVMCore.LLVMIntULT),
    LOWER_THAN_EQUAL(LLVMCore.LLVMIntSLE, LLVMCore.LLVMIntULE),
    EQUALS(LLVMCore.LLVMIntEQ),
    NOT_EQUALS(LLVMCore.LLVMIntNE);

    private final int signedOp;
    private final int unsignedOp;

    EnumCompareOp(final int signedOp, final int unsignedOp) {
        this.signedOp = signedOp;
        this.unsignedOp = unsignedOp;
    }

    EnumCompareOp(final int op) {
        this(op, op);
    }

    public int asInt(final boolean signed) {
        return signed ? this.signedOp : this.unsignedOp;
    }

}

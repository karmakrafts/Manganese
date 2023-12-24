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

import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public enum Operator {
    // @formatter:off
    // Arithmetics
    PLUS            (FerrousLexer.OP_PLUS,             "fe.op.plus",           true,   true,  false, false),
    MINUS           (FerrousLexer.OP_MINUS,            "fe.op.minus",          true,   true,  false, false),
    TIMES           (FerrousLexer.ASTERISK,            "fe.op.times",          false,  true,  false, false),
    DIV             (FerrousLexer.OP_DIV,              "fe.op.div",            false,  true,  false, false),
    MOD             (FerrousLexer.OP_MOD,              "fe.op.mod",            false,  true,  false, false),
    SAT_PLUS        (FerrousLexer.OP_SAT_PLUS,         "fe.op.splus",          false,  true,  false, false),
    SAT_MINUS       (FerrousLexer.OP_SAT_MINUS,        "fe.op.sminus",         false,  true,  false, false),
    SAT_TIMES       (FerrousLexer.OP_SAT_TIMES,        "fe.op.stimes",         false,  true,  false, false),
    SAT_DIV         (FerrousLexer.OP_SAT_DIV,          "fe.op.sdiv",           false,  true,  false, false),
    SAT_MOD         (FerrousLexer.OP_SAT_MOD,          "fe.op.smod",           false,  true,  false, false),
    PLUS_ASSIGN     (FerrousLexer.OP_PLUS_ASSIGN,      "fe.op.plus.assign",    false,  true,  true,  false),
    MINUS_ASSIGN    (FerrousLexer.OP_MINUS_ASSIGN,     "fe.op.minus.assign",   false,  true,  true,  false),
    TIMES_ASSIGN    (FerrousLexer.OP_TIMES_ASSIGN,     "fe.op.times.assign",   false,  true,  true,  false),
    DIV_ASSIGN      (FerrousLexer.OP_DIV_ASSIGN,       "fe.op.div.assign",     false,  true,  true,  false),
    MOD_ASSIGN      (FerrousLexer.OP_MOD_ASSIGN,       "fe.op.mod.assign",     false,  true,  true,  false),
    SAT_PLUS_ASSIGN (FerrousLexer.OP_SAT_PLUS_ASSIGN,  "fe.op.splus.assign",   false,  true,  true,  false),
    SAT_MINUS_ASSIGN(FerrousLexer.OP_SAT_MINUS_ASSIGN, "fe.op.sminus.assign",  false,  true,  true,  false),
    SAT_TIMES_ASSIGN(FerrousLexer.OP_SAT_TIMES_ASSIGN, "fe.op.stimes.assign",  false,  true,  true,  false),
    SAT_DIV_ASSIGN  (FerrousLexer.OP_SAT_DIV_ASSIGN,   "fe.op.sdiv.assign",    false,  true,  true,  false),
    SAT_MOD_ASSIGN  (FerrousLexer.OP_SAT_MOD_ASSIGN,   "fe.op.smod.assign",    false,  true,  true,  false),
    AND             (FerrousLexer.AMP,                 "fe.op.and",            false,  true,  false, false),
    SC_AND          (FerrousLexer.OP_SHORTC_AND,       "fe.op.scand",          false,  true,  false, false),
    OR              (FerrousLexer.PIPE,                "fe.op.or",             false,  true,  false, false),
    SC_OR           (FerrousLexer.OP_SHORTC_OR,        "fe.op.scor",           false,  true,  false, false),
    XOR             (FerrousLexer.OP_XOR,              "fe.op.xor",            false,  true,  false, false),
    SHL             (FerrousLexer.OP_LSH,              "fe.op.shl",            false,  true,  false, false),
    SHR             (FerrousLexer.OP_RSH,              "fe.op.shr",            false,  true,  false, false),
    INV             (FerrousLexer.OP_INV,              "fe.op.inv",            true,   false, false, false),
    AND_ASSIGN      (FerrousLexer.OP_AND_ASSIGN,       "fe.op.and.assign",     false,  true,  true,  false),
    OR_ASSIGN       (FerrousLexer.OP_OR_ASSIGN,        "fe.op.or.assign",      false,  true,  true,  false),
    XOR_ASSIGN      (FerrousLexer.OP_XOR_ASSIGN,       "fe.op.xor.assign",     false,  true,  true,  false),
    SHL_ASSIGN      (FerrousLexer.OP_LSH_ASSIGN,       "fe.op.shl.assign",     false,  true,  true,  false),
    SHR_ASSIGN      (FerrousLexer.OP_RSH_ASSIGN,       "fe.op.shr.assign",     false,  true,  true,  false),
    PRE_INV_ASSIGN  (FerrousLexer.OP_INV_ASSIGN,       "fe.op.inv.assign.pre", false,  true,  true,  false),
    INV_ASSIGN      (FerrousLexer.OP_INV_ASSIGN,       "fe.op.inv.assign",     false,  true,  true,  false),
    EQ              (FerrousLexer.OP_EQ,               "fe.op.eq",             false,  true,  false, false),
    NEQ             (FerrousLexer.OP_NEQ,              "fe.op.neq",            false,  true,  false, false),
    ASSIGN          (FerrousLexer.OP_ASSIGN,           "fe.op.assign",         false,  true,  true,  false),
    CMP             (FerrousLexer.OP_COMPARE,          "fe.op.cmp",            false,  true,  false, false),
    CMP_LTH         (FerrousLexer.L_CHEVRON,           "fe.op.cmp.lth",        false,  true,  false, false),
    CMP_LEQ         (FerrousLexer.OP_LEQUAL,           "fe.op.cmp.leq",        false,  true,  false, false),
    CMP_GTH         (FerrousLexer.R_CHEVRON,           "fe.op.cmp.gth",        false,  true,  false, false),
    CMP_GEQ         (FerrousLexer.OP_GEQUAL,           "fe.op.cmp.geq",        false,  true,  false, false),
    SWAP            (FerrousLexer.OP_SWAP,             "fe.op.swap",           false,  true,  true,  false),
    PRE_INC         (FerrousLexer.OP_INCREMENT,        "fe.op.inc.pre",        false,  true,  true,  false),
    INC             (FerrousLexer.OP_INCREMENT,        "fe.op.inc",            false,  true,  true,  false),
    PRE_DEC         (FerrousLexer.OP_DECREMENT,        "fe.op.dec.pre",        false,  true,  true,  false),
    DEC             (FerrousLexer.OP_DECREMENT,        "fe.op.dec",            false,  true,  true,  false),
    NOT             (FerrousLexer.OP_NOT,              "fe.op.not",            true,   false, false, false),
    DEREF           (FerrousLexer.ASTERISK,            "fe.op.deref",          true,   false, false, false),
    SAFE_DEREF      (FerrousLexer.OP_SAFE_DEREF,       "fe.op.deref.safe",     true,   false, false, false),
    PTR_REF         (FerrousLexer.ARROW,               "fe.op.ptrref",         false,  true,  false, true ),
    SAFE_PTR_REF    (FerrousLexer.OP_SAFE_PTR_REF,     "fe.op.ptrref.safe",    false,  true,  false, true ),
    MEMBER_REF      (FerrousLexer.DOT,                 "fe.op.memberref",      false,  true,  false, true ),
    REF             (FerrousLexer.AMP,                 "fe.op.ref",            true,   false, false, false),
    LABE_ADDR       (FerrousLexer.OP_LABEL_ADDR,       "fe.op.labeladdr",      true,   false, false, true)
    ;
    // @formatter:on

    private final String text;
    private final String functionName;
    private final boolean isUnary;
    private final boolean isBinary;
    private final boolean isAssignment;
    private final boolean isReference;

    Operator(final int token, final String functionName, final boolean isUnary, final boolean isBinary,
             final boolean isAssignment, final boolean isReference) {
        this.text = TokenUtils.getLiteral(token);
        this.functionName = functionName;
        this.isUnary = isUnary;
        this.isBinary = isBinary;
        this.isAssignment = isAssignment;
        this.isReference = isReference;
    }

    public static Optional<Operator> unaryByText(final String text) {
        return Arrays.stream(values()).filter(op -> op.text.equals(text) && op.isUnary).findFirst();
    }

    public static Optional<Operator> binaryByText(final String text) {
        return Arrays.stream(values()).filter(op -> op.text.equals(text) && op.isBinary).findFirst();
    }

    public int getLLVMType(final Type type) {
        final var typeKind = type.getKind();
        return switch (this) {
            case EQ -> switch (typeKind) {
                case INT, UINT, CHAR, BOOL -> LLVMCore.LLVMIntEQ;
                case REAL -> LLVMCore.LLVMRealUEQ;
                default -> 0;
            };
            case NEQ -> switch (typeKind) {
                case INT, UINT, CHAR, BOOL -> LLVMCore.LLVMIntNE;
                case REAL -> LLVMCore.LLVMRealUNE;
                default -> 0;
            };
            case CMP_GTH -> switch (typeKind) {
                case INT, CHAR -> LLVMCore.LLVMIntSGT;
                case UINT -> LLVMCore.LLVMIntUGT;
                case REAL -> LLVMCore.LLVMRealUGT;
                default -> 0;
            };
            case CMP_GEQ -> switch (typeKind) {
                case INT, CHAR -> LLVMCore.LLVMIntSGE;
                case UINT -> LLVMCore.LLVMIntUGE;
                case REAL -> LLVMCore.LLVMRealUGE;
                default -> 0;
            };
            case CMP_LTH -> switch (typeKind) {
                case INT, CHAR -> LLVMCore.LLVMIntSLT;
                case UINT -> LLVMCore.LLVMIntULT;
                case REAL -> LLVMCore.LLVMRealULT;
                default -> 0;
            };
            case CMP_LEQ -> switch (typeKind) {
                case INT, CHAR -> LLVMCore.LLVMIntSLE;
                case UINT -> LLVMCore.LLVMIntULE;
                case REAL -> LLVMCore.LLVMRealULE;
                default -> 0;
            };
            default -> 0;
        };
    }

    public boolean isUnary() {
        return isUnary;
    }

    public boolean isBinary() {
        return isBinary;
    }

    public boolean isAssignment() {
        return isAssignment;
    }

    public boolean isReference() {
        return isReference;
    }

    public String getText() {
        return text;
    }

    public String getFunctionName() {
        return functionName;
    }

    @Override
    public String toString() {
        return getText();
    }
}

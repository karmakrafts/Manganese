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

import io.karma.ferrous.vanadium.FerrousLexer;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

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
    PLUS            (FerrousLexer.OP_PLUS,              "op.plus",              false,  true ),
    MINUS           (FerrousLexer.OP_MINUS,             "op.minus",             true,   true ),
    TIMES           (FerrousLexer.ASTERISK,             "op.times",             false,  true ),
    DIV             (FerrousLexer.OP_DIV,               "op.div",               false,  true ),
    MOD             (FerrousLexer.OP_MOD,               "op.mod",               false,  true ),
    SAT_PLUS        (FerrousLexer.OP_SAT_PLUS,          "op.s.plus",            false,  true ),
    SAT_MINUS       (FerrousLexer.OP_SAT_MINUS,         "op.s.minus",           false,  true ),
    SAT_TIMES       (FerrousLexer.OP_SAT_TIMES,         "op.s.times",           false,  true ),
    SAT_DIV         (FerrousLexer.OP_SAT_DIV,           "op.s.div",             false,  true ),
    SAT_MOD         (FerrousLexer.OP_SAT_MOD,           "op.s.mod",             false,  true ),
    SAT_POW         (FerrousLexer.OP_SAT_POW,           "op.s.pow",             false,  true ),
    PLUS_ASSIGN     (FerrousLexer.OP_PLUS_ASSIGN,       "op.plus.assign",       false,  true ),
    MINUS_ASSIGN    (FerrousLexer.OP_MINUS_ASSIGN,      "op.minus.assign",      false,  true ),
    TIMES_ASSIGN    (FerrousLexer.OP_TIMES_ASSIGN,      "op.times.assign",      false,  true ),
    DIV_ASSIGN      (FerrousLexer.OP_DIV_ASSIGN,        "op.div.assign",        false,  true ),
    MOD_ASSIGN      (FerrousLexer.OP_MOD_ASSIGN,        "op.mod.assign",        false,  true ),
    POW_ASSIGN      (FerrousLexer.OP_POW_ASSIGN,        "op.pow.assign",        false,  true ),
    SAT_PLUS_ASSIGN (FerrousLexer.OP_SAT_PLUS_ASSIGN,   "op.s.plus.assign",     false,  true ),
    SAT_MINUS_ASSIGN(FerrousLexer.OP_SAT_MINUS_ASSIGN,  "op.s.minus.assign",    false,  true ),
    SAT_TIMES_ASSIGN(FerrousLexer.OP_SAT_TIMES_ASSIGN,  "op.s.times.assign",    false,  true ),
    SAT_DIV_ASSIGN  (FerrousLexer.OP_SAT_DIV_ASSIGN,    "op.s.div.assign",      false,  true ),
    SAT_MOD_ASSIGN  (FerrousLexer.OP_SAT_MOD_ASSIGN,    "op.s.mod.assign",      false,  true ),
    SAT_POW_ASSIGN  (FerrousLexer.OP_SAT_POW_ASSIGN,    "op.s.pow.assign",      false,  true ),
    AND             (FerrousLexer.AMP,                  "op.and",               false,  true ),
    SC_AND          (FerrousLexer.OP_SHORTC_AND,        "op.and.sc",            false,  true ),
    OR              (FerrousLexer.PIPE,                 "op.or",                false,  true ),
    SC_OR           (FerrousLexer.OP_SHORTC_OR,         "op.or.sc",             false,  true ),
    XOR             (FerrousLexer.OP_XOR,               "op.xor",               false,  true ),
    SHL             (FerrousLexer.OP_LSH,               "op.shl",               false,  true ),
    SHR             (FerrousLexer.OP_RSH,               "op.shr",               false,  true ),
    INV             (FerrousLexer.OP_INV,               "op.inv",               true,   false),
    AND_ASSIGN      (FerrousLexer.OP_AND_ASSIGN,        "op.and.assign",        false,  true ),
    OR_ASSIGN       (FerrousLexer.OP_OR_ASSIGN,         "op.or.assign",         false,  true ),
    XOR_ASSIGN      (FerrousLexer.OP_XOR_ASSIGN,        "op.xor.assign",        false,  true ),
    SHL_ASSIGN      (FerrousLexer.OP_LSH_ASSIGN,        "op.shl.assign",        false,  true ),
    SHR_ASSIGN      (FerrousLexer.OP_RSH_ASSIGN,        "op.shr.assign",        false,  true ),
    INV_ASSIGN      (FerrousLexer.OP_INV_ASSIGN,        "op.inv.assign",        false,  true ),
    EQ              (FerrousLexer.OP_EQ,                "op.eq",                false,  true ),
    NEQ             (FerrousLexer.OP_NEQ,               "op.neq",               false,  true ),
    ASSIGN          (FerrousLexer.OP_ASSIGN,            "op.assign",            false,  true ),
    CMP             (FerrousLexer.OP_COMPARE,           "op.cmp",               false,  true ),
    CMP_LTH         (FerrousLexer.L_CHEVRON,            "op.cmp.lth",           false,  true ),
    CMP_LEQ         (FerrousLexer.OP_LEQUAL,            "op.cmp.leq",           false,  true ),
    CMP_GTH         (FerrousLexer.R_CHEVRON,            "op.cmp.gth",           false,  true ),
    CMP_GEQ         (FerrousLexer.OP_GEQUAL,            "op.cmp.geq",           false,  true ),
    SWAP            (FerrousLexer.OP_SWAP,              "op.swap",              false,  true ),
    INC             (FerrousLexer.OP_INCREMENT,         "op.inc",               false,  true ),
    PRE_INC         (FerrousLexer.OP_INCREMENT,         "op.inc.pre",           false,  true ),
    DEC             (FerrousLexer.OP_DECREMENT,         "op.dec",               false,  true ),
    PRE_DEC         (FerrousLexer.OP_DECREMENT,         "op.dec.pre",           false,  true )
    ;
    // @formatter:on

    private final String text;
    private final String functionName;
    private final boolean isUnary;
    private final boolean isBinary;

    Operator(final int token, final String functionName, final boolean isUnary, final boolean isBinary) {
        this.text = TokenUtils.getLiteral(token);
        this.functionName = functionName;
        this.isUnary = isUnary;
        this.isBinary = isBinary;
    }

    public static Optional<Operator> findByText(final String text) {
        return Arrays.stream(values()).filter(op -> op.text.equals(text)).findFirst();
    }

    public boolean isUnary() {
        return isUnary;
    }

    public boolean isBinary() {
        return isBinary;
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

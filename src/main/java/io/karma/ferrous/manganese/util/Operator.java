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
    PLUS            (FerrousLexer.OP_PLUS,              "op.plus",              true,   true),
    MINUS           (FerrousLexer.OP_MINUS,             "op.minus",             true,   true),
    TIMES           (FerrousLexer.ASTERISK,             "op.times",             false,  true),
    DIV             (FerrousLexer.OP_DIV,               "op.div",               false,  true),
    MOD             (FerrousLexer.OP_MOD,               "op.mod",               false,  true),
    POW             (FerrousLexer.OP_POW,               "op.pow",               false,  true),
    SAT_PLUS        (FerrousLexer.OP_SAT_PLUS,          "op.sat_plus",          false,  true),
    SAT_MINUS       (FerrousLexer.OP_SAT_MINUS,         "op.sat_minus",         false,  true),
    SAT_TIMES       (FerrousLexer.OP_SAT_TIMES,         "op.sat_times",         false,  true),
    SAT_DIV         (FerrousLexer.OP_SAT_DIV,           "op.sat_div",           false,  true),
    SAT_MOD         (FerrousLexer.OP_SAT_MOD,           "op.sat_mod",           false,  true),
    SAT_POW         (FerrousLexer.OP_SAT_POW,           "op.sat_pow",           false,  true),
    PLUS_ASSIGN     (FerrousLexer.OP_PLUS_ASSIGN,       "op.plus_assign",       false,  true),
    MINUS_ASSIGN    (FerrousLexer.OP_MINUS_ASSIGN,      "op.minus_assign",      false,  true),
    TIMES_ASSIGN    (FerrousLexer.OP_TIMES_ASSIGN,      "op.times_assign",      false,  true),
    DIV_ASSIGN      (FerrousLexer.OP_DIV_ASSIGN,        "op.div_assign",        false,  true),
    MOD_ASSIGN      (FerrousLexer.OP_MOD_ASSIGN,        "op.mod_assign",        false,  true),
    POW_ASSIGN      (FerrousLexer.OP_POW_ASSIGN,        "op.pow_assign",        false,  true),
    SAT_PLUS_ASSIGN (FerrousLexer.OP_SAT_PLUS_ASSIGN,   "op.sat_plus_assign",   false,  true),
    SAT_MINUS_ASSIGN(FerrousLexer.OP_SAT_MINUS_ASSIGN,  "op.sat_minus_assign",  false,  true),
    SAT_TIMES_ASSIGN(FerrousLexer.OP_SAT_TIMES_ASSIGN,  "op.sat_times_assign",  false,  true),
    SAT_DIV_ASSIGN  (FerrousLexer.OP_SAT_DIV_ASSIGN,    "op.sat_div_assign",    false,  true),
    SAT_MOD_ASSIGN  (FerrousLexer.OP_SAT_MOD_ASSIGN,    "op.sat_mod_assign",    false,  true),
    SAT_POW_ASSIGN  (FerrousLexer.OP_SAT_POW_ASSIGN,    "op.sat_pow_assign",    false,  true),
    AND             (FerrousLexer.AMP,                  "op.and",               false,  true),
    SC_AND          (FerrousLexer.OP_SHORTC_AND,        "op.sc_and",            false,  true),
    OR              (FerrousLexer.PIPE,                 "op.or",                false,  true),
    SC_OR           (FerrousLexer.OP_SHORTC_OR,         "op.sc_or",             false,  true),
    XOR             (FerrousLexer.OP_XOR,               "op.xor",               false,  true),
    SHL             (FerrousLexer.OP_LSH,               "op.shl",               false,  true),
    SHR             (FerrousLexer.OP_RSH,               "op.shr",               false,  true),
    INV             (FerrousLexer.OP_INV,               "op.inv",               true,   false),
    AND_ASSIGN      (FerrousLexer.OP_AND_ASSIGN,        "op.and_assign",        false,  true),
    OR_ASSIGN       (FerrousLexer.OP_OR_ASSIGN,         "op.or_assign",         false,  true),
    XOR_ASSIGN      (FerrousLexer.OP_XOR_ASSIGN,        "op.xor_assign",        false,  true),
    SHL_ASSIGN      (FerrousLexer.OP_LSH_ASSIGN,        "op.shl_assign",        false,  true),
    SHR_ASSIGN      (FerrousLexer.OP_RSH_ASSIGN,        "op.shr_assign",        false,  true),
    INV_ASSIGN      (FerrousLexer.OP_INV_ASSIGN,        "op.inv_assign",        false,  true)
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

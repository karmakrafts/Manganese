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

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public enum Operator {
    // @formatter:off
    // Arithmetics
    PLUS            (FerrousLexer.OP_PLUS,              "fe.plus"),
    MINUS           (FerrousLexer.OP_MINUS,             "fe.minus"),
    TIMES           (FerrousLexer.ASTERISK,             "fe.times"),
    DIV             (FerrousLexer.OP_DIV,               "fe.div"),
    MOD             (FerrousLexer.OP_MOD,               "fe.mod"),
    POW             (FerrousLexer.OP_POW,               "fe.pow"),
    // Saturating arithmetics
    SAT_PLUS        (FerrousLexer.OP_SAT_PLUS,          "fe.sat_plus"),
    SAT_MINUS       (FerrousLexer.OP_SAT_MINUS,         "fe.sat_minus"),
    SAT_TIMES       (FerrousLexer.OP_SAT_TIMES,         "fe.sat_times"),
    SAT_DIV         (FerrousLexer.OP_SAT_DIV,           "fe.sat_div"),
    SAT_MOD         (FerrousLexer.OP_SAT_MOD,           "fe.sat_mod"),
    SAT_POW         (FerrousLexer.OP_SAT_POW,           "fe.sat_pow"),
    // Assignment arithmetics
    PLUS_ASSIGN     (FerrousLexer.OP_PLUS_ASSIGN,       "fe.plus_assign"),
    MINUS_ASSIGN    (FerrousLexer.OP_MINUS_ASSIGN,      "fe.minus_assign"),
    TIMES_ASSIGN    (FerrousLexer.OP_TIMES_ASSIGN,      "fe.times_assign"),
    DIV_ASSIGN      (FerrousLexer.OP_DIV_ASSIGN,        "fe.div_assign"),
    MOD_ASSIGN      (FerrousLexer.OP_MOD_ASSIGN,        "fe.mod_assign"),
    POW_ASSIGN      (FerrousLexer.OP_POW_ASSIGN,        "fe.pow_assign"),
    // Saturated assignment arithmetics
    SAT_PLUS_ASSIGN (FerrousLexer.OP_SAT_PLUS_ASSIGN,   "fe.sat_plus_assign"),
    SAT_MINUS_ASSIGN(FerrousLexer.OP_SAT_MINUS_ASSIGN,  "fe.sat_minus_assign"),
    SAT_TIMES_ASSIGN(FerrousLexer.OP_SAT_TIMES_ASSIGN,  "fe.sat_times_assign"),
    SAT_DIV_ASSIGN  (FerrousLexer.OP_SAT_DIV_ASSIGN,    "fe.sat_div_assign"),
    SAT_MOD_ASSIGN  (FerrousLexer.OP_SAT_MOD_ASSIGN,    "fe.sat_mod_assign"),
    SAT_POW_ASSIGN  (FerrousLexer.OP_SAT_POW_ASSIGN,    "fe.sat_pow_assign"),
    // Pointer call
    ;
    // @formatter:on

    private final String text;
    private final String functionName;

    Operator(final int token, final String functionName) {
        this.text = TokenUtils.getLiteral(token);
        this.functionName = functionName;
    }

    public static Optional<Operator> findByText(final String text) {
        return Arrays.stream(values()).filter(op -> op.text.equals(text)).findFirst();
    }

    public String getText() {
        return text;
    }

    public String getFunctionName() {
        return functionName;
    }
}

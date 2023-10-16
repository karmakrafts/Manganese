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

package io.karma.ferrous.manganese;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;

/**
 * Represents all possible outcomes of a compilation.
 * Results go from good to bad sequentially from top to bottom.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public enum CompileStatus {
    // @formatter:off
    SKIPPED              (0, true,  Color.GREEN,  "Compilation Skipped"),
    SUCCESS              (0, true,  Color.GREEN,  "Compilation Successful"),
    SUCCESS_WITH_WARNINGS(0, true,  Color.YELLOW, "Compilation Successful (with warnings)"),
    IO_ERROR             (1, false, Color.RED,    "Compilation Failed (IO error)"),
    SYNTAX_ERROR         (1, false, Color.RED,    "Compilation Failed (syntax error)"),
    SEMANTIC_ERROR       (1, false, Color.RED,    "Compilation Failed (semantic error)"),
    ANALYZER_ERROR       (1, false, Color.RED,    "Compilation Failed (analyzer error)"),
    TRANSLATION_ERROR    (1, false, Color.RED,    "Compilation Failed (translation error)"),
    VERIFY_ERROR         (1, false, Color.RED,    "Compilation Failed (verify error)"),
    UNKNOWN_ERROR        (1, false, Color.RED,    "Compilation Failed (unknown error)");
    // @formatter:on

    private final Color color;
    private final String message;
    private final boolean isRecoverable;
    private final int exitCode;

    CompileStatus(final int exitCode, final boolean isRecoverable, final Color color, final String message) {
        this.exitCode = exitCode;
        this.isRecoverable = isRecoverable;
        this.color = color;
        this.message = message;
    }

    public CompileStatus worse(final CompileStatus result) {
        if (!isRecoverable && result.isRecoverable) {
            return this;
        }

        if (isRecoverable && !result.isRecoverable) {
            return result;
        }

        return ordinal() < result.ordinal() ? result : this;
    }

    public Color getColor() {
        return color;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRecoverable() {
        return isRecoverable;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getFormattedMessage() { // @formatter:off
        return Ansi.ansi()
            .fg(color)
            .a(Attribute.INTENSITY_BOLD)
            .a(message)
            .a(Attribute.RESET)
            .toString();
    } // @formatter:on
}

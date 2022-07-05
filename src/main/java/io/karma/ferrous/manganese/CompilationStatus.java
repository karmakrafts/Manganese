package io.karma.ferrous.manganese;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.NotNull;

/**
 * Represents all possible outcomes of a compilation.
 * Results go from good to bad sequentially from top to bottom.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public enum CompilationStatus {
    // @formatter:off
    SKIPPED                 (0,   true,  Color.GREEN, "Compilation Skipped"),
    SUCCESS                 (0,   true,  Color.GREEN, "Compilation Successful"),
    SUCCESS_WITH_WARNINGS   (0,   true,  Color.GREEN, "Compilation Successful (with warnings)"),
    IO_ERROR                (1,   false, Color.RED,   "Compilation Failed (IO error)"),
    SYNTAX_ERROR            (2,   false, Color.RED,   "Compilation Failed (syntax error)"),
    TRANSLATION_ERROR       (3,   false, Color.RED,   "Compilation Failed (translation error)"),
    UNKNOWN_ERROR           (999, false, Color.RED,   "Compilation Failed (unknown error)");
    // @formatter:on

    private final Color color;
    private final String message;
    private final boolean isRecoverable;
    private final int exitCode;

    CompilationStatus(final int exitCode, final boolean isRecoverable, final @NotNull Color color, final @NotNull String message) {
        this.exitCode = exitCode;
        this.isRecoverable = isRecoverable;
        this.color = color;
        this.message = message;
    }

    public @NotNull CompilationStatus worse(final @NotNull CompilationStatus result) {
        if (!isRecoverable && result.isRecoverable) {
            return this;
        }

        if (isRecoverable && !result.isRecoverable) {
            return result;
        }

        return ordinal() < result.ordinal() ? result : this;
    }

    public @NotNull Color getColor() {
        return color;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public boolean isRecoverable() {
        return isRecoverable;
    }

    public int getExitCode() {
        return exitCode;
    }

    public @NotNull String getFormattedMessage() { // @formatter:off
        return Ansi.ansi()
            .fg(color)
            .a(Attribute.INTENSITY_BOLD)
            .a(message)
            .a(Attribute.RESET)
            .toString();
    } // @formatter:on
}

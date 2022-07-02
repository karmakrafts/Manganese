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
    SKIPPED                 (Color.GREEN,   "Compilation Skipped"),
    SUCCESS                 (Color.GREEN,   "Compilation Successful"),
    SUCCESS_WITH_WARNINGS   (Color.GREEN,   "Compilation Successful (with warnings)"),
    IO_ERROR                (Color.RED,     "Compilation Failed (IO error)"),
    SYNTAX_ERROR            (Color.RED,     "Compilation Failed (syntax error)"),
    UNKNOWN_ERROR           (Color.RED,     "Compilation Failed (unknown error)");
    // @formatter:on

    private final Color color;
    private final String message;

    CompilationStatus(final @NotNull Color color, final @NotNull String message) {
        this.color = color;
        this.message = message;
    }

    public @NotNull CompilationStatus worse(final @NotNull CompilationStatus result) {
        return ordinal() < result.ordinal() ? result : this;
    }

    public @NotNull Color getColor() {
        return color;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public @NotNull String getFormattedMessage() {
        return Ansi.ansi().fg(color).a(Attribute.INTENSITY_BOLD).a(message).a(Attribute.RESET).toString();
    }
}

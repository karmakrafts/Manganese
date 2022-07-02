package io.karma.ferrous.manganese;

import org.jetbrains.annotations.NotNull;

/**
 * Represents all possible outcomes of a compilation.
 * Results go from good to bad sequentially from top to bottom.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
public enum CompilationResult {
    SKIPPED,
    SUCCESS,
    SUCCESS_WITH_WARNINGS,
    SYNTAX_ERROR,
    UNKNOWN_ERROR;

    public @NotNull CompilationResult worse(final @NotNull CompilationResult result) {
        return ordinal() < result.ordinal() ? result : this;
    }
}

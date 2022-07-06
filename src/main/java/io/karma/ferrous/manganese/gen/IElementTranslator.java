package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.tree.IFIRElement;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides functionality for converting a given parse tree
 * element into FIR bytecode.
 *
 * @author Alexander Hinze
 * @since 05/07/2022
 */
@FunctionalInterface
public interface IElementTranslator {
    @Nullable IFIRElement<?, ?> translate(final @NotNull ITranslationContext ctx, final @NotNull ParseTree node);
}

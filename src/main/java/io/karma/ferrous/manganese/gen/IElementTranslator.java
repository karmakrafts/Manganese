package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.tree.IFIRElement;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

/**
 * Provides functionality for converting a given parse tree
 * element into FIR bytecode.
 *
 * @author Alexander Hinze
 * @since 05/07/2022
 */
public interface IElementTranslator<E extends IFIRElement<E, ?>> {
    @NotNull E translate(final @NotNull ParseTree ctx);
}

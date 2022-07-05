package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.tree.block.impl.FIRFileBlock;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public final class FileTranslator implements IElementTranslator<FIRFileBlock> {
    @Override
    public @NotNull FIRFileBlock translate(final @NotNull ParseTree ctx) {
        final var block = new FIRFileBlock();
        block.setName("entry");
        return block;
    }
}

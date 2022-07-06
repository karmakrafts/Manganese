package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.codec.IFIRCodec;
import io.karma.ferrous.fir.tree.IFIRElement;
import io.karma.ferrous.fir.tree.block.impl.FIRClassBlock;
import io.karma.ferrous.vanadium.FerrousParser.Class_declContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public final class ClassTranslator implements IElementTranslator {
    @SuppressWarnings("unchecked")
    @Override
    public <E extends IFIRElement<E, C>, C extends IFIRCodec<E>> @NotNull E translate(final @NotNull ITranslationContext ctx, final @NotNull ParseTree node) {
        final var classCtx = (Class_declContext) node;
        final var block = new FIRClassBlock();
        block.setName(classCtx.ident().getText()); // Derive class name
        return (E)block;
    }
}

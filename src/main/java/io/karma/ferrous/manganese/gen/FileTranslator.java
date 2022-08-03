package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.tree.IFIRElement;
import io.karma.ferrous.fir.tree.block.impl.FIRFileBlock;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;

public final class FileTranslator implements IElementTranslator {
    @Override
    public @NotNull IFIRElement<?, ?> translate(final @NotNull ITranslationContext ctx, final @NotNull ParseTree node) {
        final var fileCtx = (FileContext) node;
        final var block = new FIRFileBlock();
        block.setName("entry");

        final var pkgCtx = fileCtx.package_decl(); // TODO
        final var declarations = fileCtx.decl();

        for (final var decl : declarations) {
            final var translator = BytecodeGenerator.getTranslator(decl.getClass());

            if (translator == null) {
                Logger.INSTANCE.warn("Encountered unsupported element, this is not a bug, developers were being lazy");
                continue; // Skip unsupported entry
            }

            final var element = translator.translate(ctx, decl);
            block.addChild(element);
        }

        return block;
    }
}

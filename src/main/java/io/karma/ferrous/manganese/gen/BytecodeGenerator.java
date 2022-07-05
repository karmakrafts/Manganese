package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.FIR;
import io.karma.ferrous.fir.tree.IFIRElement;
import io.karma.ferrous.vanadium.FerrousParser.EvalContext;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import io.karma.ferrous.vanadium.FerrousParser.Script_fileContext;
import io.karma.kommons.io.stream.IMemoryStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public final class BytecodeGenerator {
    private static final HashMap<Class<? extends ParseTree>, IElementTranslator<?>> TRANSLATORS = new HashMap<>();

    static {
        TRANSLATORS.put(FileContext.class, new FileTranslator());
        TRANSLATORS.put(Script_fileContext.class, new ScriptFileTranslator());
    }

    @SuppressWarnings("unchecked")
    static <E extends IFIRElement<E, ?>> @Nullable IElementTranslator<E> getTranslator(final @NotNull Class<? extends ParseTree> type) {
        final var entries = TRANSLATORS.entrySet();

        for (final var entry : entries) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue; // Skip, as this is not out type
            }

            return (IElementTranslator<E>) entry.getValue();
        }

        return null;
    }

    public boolean generate(final @NotNull EvalContext ctx, final @NotNull IMemoryStream stream) {
        if (ctx.getChildCount() != 1) {
            return false;
        }

        final var fileCtx = ctx.getChild(0);
        final var translator = getTranslator(fileCtx.getClass());

        if (translator == null) {
            return false;
        }

        final var element = translator.translate(fileCtx);
        FIR.generate(element.asBlock(), stream);
        return true;
    }

    public void reset() {
    }
}

package io.karma.ferrous.manganese.gen;

import io.karma.ferrous.fir.FIR;
import io.karma.ferrous.manganese.Manganese;
import io.karma.ferrous.vanadium.FerrousParser.Attrib_declContext;
import io.karma.ferrous.vanadium.FerrousParser.Class_declContext;
import io.karma.ferrous.vanadium.FerrousParser.EvalContext;
import io.karma.ferrous.vanadium.FerrousParser.FileContext;
import io.karma.ferrous.vanadium.FerrousParser.Iface_declContext;
import io.karma.ferrous.vanadium.FerrousParser.Trait_declContext;
import io.karma.ferrous.vanadium.FerrousParser.Udt_declContext;
import io.karma.kommons.io.stream.IMemoryStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

public final class BytecodeGenerator {
    private static final HashMap<Class<? extends ParseTree>, IElementTranslator> TRANSLATORS = new HashMap<>();
    private static final HashMap<Class<? extends ParseTree>, ArrayList<Class<? extends ParseTree>>> DELEGATIONS = new HashMap<>();

    static {
        TRANSLATORS.put(FileContext.class, new FileTranslator());

        TRANSLATORS.put(Class_declContext.class, new ClassTranslator());
        TRANSLATORS.put(Iface_declContext.class, new InterfaceTranslator());
        TRANSLATORS.put(Attrib_declContext.class, new AttributeTranslator());
        TRANSLATORS.put(Trait_declContext.class, new TraitTranslator());

        addDelegation(Udt_declContext.class, Class_declContext.class, Iface_declContext.class, Attrib_declContext.class, Trait_declContext.class);
    }

    private final Manganese compiler;
    private final ContextImpl context = new ContextImpl();

    public BytecodeGenerator(final @NotNull Manganese compiler) {
        this.compiler = compiler;
    }

    @SafeVarargs
    private static void addDelegation(final @NotNull Class<? extends ParseTree> type, final Class<? extends ParseTree>... delegateTypes) {
        if (DELEGATIONS.containsKey(type)) {
            throw new IllegalStateException("Delegation already exists");
        }

        DELEGATIONS.put(type, new ArrayList<>(Arrays.asList(delegateTypes)));
        TRANSLATORS.put(type, createDelegatingTranslator());
    }

    private static @NotNull IElementTranslator createDelegatingTranslator() {
        return (ctx, node) -> {
            final var numChildren = node.getChildCount();
            final var nodeType = node.getClass();

            for (var i = 0; i < numChildren; i++) {
                final var child = node.getChild(i);
                final var childType = child.getClass();
                final var translator = getTranslator(childType);
                final var element = translator.translate(ctx, child);
            }

            return null;
        };
    }

    static @Nullable IElementTranslator getTranslator(final @NotNull Class<? extends ParseTree> type) {
        final var entries = TRANSLATORS.entrySet();

        for (final var entry : entries) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue; // Skip, as this is not out type
            }

            return entry.getValue();
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

        final var element = translator.translate(context, fileCtx);
        FIR.generate(element.asBlock(), stream);
        return true;
    }

    public void reset() {
    }

    public @NotNull Manganese getCompiler() {
        return compiler;
    }

    private final class ContextImpl implements ITranslationContext {
        @Override
        public @NotNull Optional<Path> getSourcePath() {
            return compiler.getSourcePath();
        }

        @Override
        public @NotNull Optional<Path> getOutputPath() {
            return compiler.getOutputPath();
        }
    }
}

package io.karma.ferrous.manganese.gen;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;

public interface ITranslationContext {
    @NotNull Optional<Path> getSourcePath();

    @NotNull Optional<Path> getOutputPath();
}

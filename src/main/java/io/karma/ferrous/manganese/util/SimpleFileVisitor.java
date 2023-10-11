package io.karma.ferrous.manganese.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
public final class SimpleFileVisitor implements FileVisitor<Path> {
    private final Function<Path, FileVisitResult> callback;

    public SimpleFileVisitor(final Function<Path, FileVisitResult> callback) {
        this.callback = callback;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        return callback.apply(file);
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        return FileVisitResult.CONTINUE;
    }
}

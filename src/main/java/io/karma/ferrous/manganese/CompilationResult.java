package io.karma.ferrous.manganese;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains the status and a list of paths to
 * some compiled files as a result of a compilation.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
public final class CompilationResult {
    private final CompilationStatus status;
    private final List<Path> compiledFiles;

    public CompilationResult(final CompilationStatus status, final List<Path> compiledFiles) {
        this.status = status;
        this.compiledFiles = compiledFiles;
    }

    public CompilationResult(final CompilationStatus status, final Path... compiledFiles) {
        this(status, Arrays.asList(compiledFiles));
    }

    public CompilationStatus getStatus() {
        return status;
    }

    public List<Path> getCompiledFiles() {
        return compiledFiles;
    }

    public int getCompiledFileCount() {
        return compiledFiles.size();
    }

    public CompilationResult merge(final CompilationResult other) {
        final var status = this.status.worse(other.status);
        final var compiledFiles = new ArrayList<>(this.compiledFiles);
        compiledFiles.addAll(other.compiledFiles);
        return new CompilationResult(status, compiledFiles);
    }
}

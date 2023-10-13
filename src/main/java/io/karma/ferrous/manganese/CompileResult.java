/*
 * Copyright 2023 Karma Krafts & associates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.karma.ferrous.manganese;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the status and a list of paths to
 * some compiled files as a result of a compilation.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
public final class CompileResult {
    private final CompileStatus status;
    private final List<Path> compiledFiles;
    private final List<CompileError> errors;

    public CompileResult(final CompileStatus status, final List<Path> compiledFiles, final List<CompileError> errors) {
        this.status = status;
        this.compiledFiles = compiledFiles;
        this.errors = errors;
    }

    public CompileStatus getStatus() {
        return status;
    }

    public List<Path> getCompiledFiles() {
        return compiledFiles;
    }

    public int getCompiledFileCount() {
        return compiledFiles.size();
    }

    public List<CompileError> getErrors() {
        return errors;
    }

    public CompileResult merge(final CompileResult other) {
        final var status = this.status.worse(other.status);
        final var compiledFiles = new ArrayList<>(this.compiledFiles);
        compiledFiles.addAll(other.compiledFiles);
        final var errors = new ArrayList<>(this.errors);
        errors.addAll(other.errors);
        return new CompileResult(status, compiledFiles, errors);
    }
}

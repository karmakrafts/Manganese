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

package io.karma.ferrous.manganese.linker;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.SimpleFileVisitor;
import io.karma.kommons.function.Functions;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class ELFLinker extends AbstractLinker {
    private static final String[] LIB_DIRS = {"/usr", "/usr/local"};
    private Path systemLibDir;

    ELFLinker() {
        super(KitchenSink.allExcept(Architecture.class, Architecture.WASM32, Architecture.WASM64));
    }

    private void handleLibraries(final ArrayList<String> buffer, final LinkModel linkModel,
                                 final Architecture architecture, final boolean usesSystemRuntime) {
        if (usesSystemRuntime) {
            final var sysLibDir = Objects.requireNonNull(findSystemLibraryDirectory(architecture));
            final var path = sysLibDir.resolve("lib").toAbsolutePath().normalize().toString();
            buffer.add(STR."-L\{path}");
            buffer.add("-lc");
        }
    }

    private void handlePreObject(final CompileContext compileContext, final ArrayList<String> buffer,
                                 final LinkModel linkModel, final Architecture architecture,
                                 final boolean usesSystemRuntime) {
        if (usesSystemRuntime) {
            // CRT implementation
            final var crtImplPath = findSystemLibrary(architecture, path -> path.resolve("lib"), "crt[012]\\.o");
            if (crtImplPath == null) {
                compileContext.reportError("CRT", CompileErrorCode.E6009);
                return;
            }
            Logger.INSTANCE.debugln(STR."Located CRT implementation at \{crtImplPath}");
            buffer.add(crtImplPath.toAbsolutePath().normalize().toString());
            // Prologue object
            final var crtProloguePath = findSystemLibrary(architecture, path -> path.resolve("lib"), "crti\\.o");
            if (crtProloguePath == null) {
                compileContext.reportError("CRT Prologue", CompileErrorCode.E6009);
                return;
            }
            Logger.INSTANCE.debugln(STR."Located CRT prologue at \{crtProloguePath}");
            buffer.add(crtProloguePath.toAbsolutePath().normalize().toString());
        }
    }

    private void handlePostObject(final CompileContext compileContext, final ArrayList<String> buffer,
                                  final LinkModel linkModel, final Architecture architecture,
                                  final boolean usesSystemRuntime) {
        if (usesSystemRuntime) {
            // Epilogue object
            final var crtEpiloguePath = findSystemLibrary(architecture, path -> path.resolve("lib"), "crtn\\.o");
            if (crtEpiloguePath == null) {
                compileContext.reportError("CRT Epilogue", CompileErrorCode.E6009);
                return;
            }
            Logger.INSTANCE.debugln(STR."Located CRT epilogue at \{crtEpiloguePath}");
            buffer.add(crtEpiloguePath.toAbsolutePath().normalize().toString());
        }
    }

    private @Nullable Path findSystemLibraryDirectory(final Architecture architecture) {
        if (systemLibDir != null) {
            return systemLibDir;
        }
        for (var directory : LIB_DIRS) {
            for (final var alias : architecture.getAliases()) {
                final var path = Path.of(directory).resolve(STR."\{alias}-linux-gnu");
                if (!Files.exists(path)) {
                    continue;
                }
                return systemLibDir = path;
            }
        }
        return null;
    }

    private @Nullable Path findSystemLibrary(final Architecture architecture, final Function<Path, Path> mapper,
                                             final String pattern) {
        final var sysLibDir = findSystemLibraryDirectory(architecture);
        if (sysLibDir == null) {
            return null;
        }
        try {
            final var predicate = Pattern.compile(pattern).asMatchPredicate();
            final var pathRef = new Path[1];
            Files.walkFileTree(mapper.apply(sysLibDir), new SimpleFileVisitor(path -> {
                if (Files.isDirectory(path) || !predicate.test(path.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                pathRef[0] = path;
                return FileVisitResult.TERMINATE;
            }));
            return pathRef[0];
        }
        catch (Exception error) {
            return null;
        }
    }

    @Override
    protected void buildCommand(final ArrayList<String> buffer, final String command, final Path outFile,
                                final Path objectFile, final LinkModel linkModel, final TargetMachine targetMachine,
                                final CompileContext compileContext, final LinkTargetType targetType) {
        final var isDynamic = targetMachine.getRelocation().isDynamic();
        final var usesSystemRuntime = linkModel == LinkModel.FULL;
        if (usesSystemRuntime && !isDynamic) {
            compileContext.reportError(CompileErrorCode.E6006);
            return;
        }
        final var architecture = targetMachine.getTarget().getArchitecture();
        if (usesSystemRuntime && targetType == LinkTargetType.STATIC) {
            compileContext.reportError(CompileErrorCode.E6008);
            return;
        }
        buffer.add(command);
        switch (targetType) {
            case SHARED:
                buffer.add("-shared");
                break;
            case STATIC:
                buffer.add("-static");
                break;
        }
        if (isDynamic) {
            buffer.add("-dynamic-linker");
            final var path = findSystemLibrary(architecture,
                Functions.castingIdentity(),
                STR."ld[\\-_.]linux[\\-_.](\{architecture.makePattern()})(\\.so(\\.[0-9])?)");
            if (path == null) {
                compileContext.reportError(CompileErrorCode.E6007);
                return;
            }
            Logger.INSTANCE.debugln(STR."Located dynamic linker at \{path}");
            buffer.add(path.toAbsolutePath().normalize().toString());
        }
        buffer.addAll(options);
        buffer.add("-o");
        buffer.add(outFile.toAbsolutePath().normalize().toString());
        handlePreObject(compileContext, buffer, linkModel, architecture, usesSystemRuntime);
        handleLibraries(buffer, linkModel, architecture, usesSystemRuntime);
        buffer.add(objectFile.toAbsolutePath().normalize().toString());
        handlePostObject(compileContext, buffer, linkModel, architecture, usesSystemRuntime);
    }

    @Override
    public LinkerType getType() {
        return LinkerType.ELF;
    }
}

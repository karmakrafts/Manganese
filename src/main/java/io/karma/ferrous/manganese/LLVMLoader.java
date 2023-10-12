package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.SimpleFileVisitor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexander Hinze
 * @since 12/10/2023
 */
public final class LLVMLoader {
    private static final Path[] LLVM_LIB_DIRECTORIES = {Path.of("/usr/lib")};
    private static final HashSet<String> LLVM_LIB_FILE_NAMES = new HashSet<>(Arrays.asList("libLLVM.so", "libLLVM.dylib", "LLVM.dll"));
    private static final AtomicBoolean ARE_NATIVES_LOADED = new AtomicBoolean(false);

    static void ensureNativesLoaded() {
        if (ARE_NATIVES_LOADED.compareAndSet(false, true)) {
            try {
                var libraryPath = System.getProperty("org.lwjgl.llvm.libname");
                if ((libraryPath == null || libraryPath.isEmpty()) && System.getProperty("org.lwjgl.librarypath") == null) {
                    final var candidates = new ArrayList<>();
                    for (final var directory : LLVM_LIB_DIRECTORIES) {
                        if (!Files.exists(directory)) {
                            continue;
                        }
                        try {
                            Files.walkFileTree(directory, new SimpleFileVisitor(filePath -> {
                                if (!LLVM_LIB_FILE_NAMES.contains(filePath.getFileName().toString())) {
                                    return FileVisitResult.CONTINUE;
                                }
                                Logger.INSTANCE.debug("Found LLVM library candidate at %s", filePath.toString());
                                candidates.add(filePath);
                                return FileVisitResult.CONTINUE;
                            }));
                        }
                        catch (IOException error) { /* swallow exception */ }
                    }
                    candidates.sort(Comparator.comparing(Object::toString));
                    libraryPath = candidates.get(candidates.size() - 1).toString();
                }
                if (libraryPath != null) {
                    Logger.INSTANCE.debug("Using LLVM library at %s", libraryPath);
                    System.setProperty("org.lwjgl.llvm.libname", libraryPath);
                }
                else {
                    Logger.INSTANCE.error("Could not locate LLVM library, aborting");
                    System.exit(1);
                }
            }
            catch (UnsatisfiedLinkError error) { // @formatter:off
                Logger.INSTANCE.error("Please configure the LLVM shared libraries path with:\n"
                                      + "\t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or\n"
                                      + "\t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>\n\t%s", error);
            } // @formatter:on
        }
    }
}

package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.kommons.function.Functions;
import io.karma.kommons.io.stream.IMemoryStream;
import io.karma.kommons.io.stream.MemoryStream;
import io.karma.kommons.util.ExceptionUtils;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main class for invoking a compilation, either programmatically
 * or via a command line interface through the given main entry point.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Manganese {
    private static boolean IS_STANDALONE = false;

    // @formatter:off
    private Manganese() {}
    // @formatter:on

    // Main-entry point which invokes Manganese as a standalone tool
    @API(status = Status.INTERNAL)
    public static void main(final @NotNull String[] args) {
        Logger.INSTANCE.init(); // Initialize logger beforehand so we can catch all errors

        Functions.tryDo(() -> {
            final var parser = new OptionParser("?io");
            //parser.formatHelpWith(Logger.INSTANCE);
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.");
            parser.accepts("o", "A FIR file or a directory in which to save the compiled FIR bytecode.");
            final var options = parser.parse(args);

            if (!options.has("i") || options.has("?")) {
                parser.printHelpOn(Logger.INSTANCE); // Print help
                return;
            }

            final var in = Paths.get((String) options.valueOf("i")).toAbsolutePath().normalize();
            final var out = Paths.get((String) options.valueOf("o")).toAbsolutePath().normalize();

            IS_STANDALONE = true;
            compile(in, out);
        }, (final Throwable e) -> ExceptionUtils.handleError(e, Logger.INSTANCE::error));
    }

    public static @NotNull CompilationResult compile(final @NotNull Path in, final @NotNull Path out) {
        final var inFile = in.toFile();
        var outFile = out.toFile();

        if (!inFile.exists()) {
            Logger.INSTANCE.warn("%s does not exist, skipping", inFile.getAbsolutePath());
            return CompilationResult.SKIPPED;
        }

        if (inFile.isDirectory()) {
            if (!outFile.isDirectory()) {
                Logger.INSTANCE.warn("%s is not a directory, skipping", outFile.getAbsolutePath());
                return CompilationResult.SKIPPED;
            }

            final var subFiles = inFile.listFiles();
            var result = CompilationResult.SKIPPED;

            if (subFiles == null || subFiles.length == 0) {
                return result; // Skip empty directory
            }

            for (final var subFile : subFiles) {
                final var subInPath = subFile.toPath();
                // TODO
            }

            return result;
        }
        else {
            // If the output path is a directory, use the input path's file name
            if (outFile.isDirectory()) {
                final var fileName = ""; // TODO
                outFile = new File(outFile, fileName);
            }

            // @formatter:off
            try (final var fis = new FileInputStream(inFile)) {
                final var inStream = IMemoryStream.fromStream(MemoryStream::new, fis);

                try(final var fos = new FileOutputStream(outFile)) {
                    final var outStream = new MemoryStream();
                }
            }
            catch (Throwable t) {
                ExceptionUtils.handleError(t, Logger.INSTANCE::error);
                return CompilationResult.UNKNOWN_ERROR;
            }
            // @formatter:on
        }

        return CompilationResult.SUCCESS;
    }

    public static @NotNull CompilationResult compile(final @NotNull IMemoryStream in, final @NotNull IMemoryStream out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded
        return CompilationResult.SUCCESS;
    }

    public static boolean isEmbedded() {
        return !IS_STANDALONE;
    }
}

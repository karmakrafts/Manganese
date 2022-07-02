package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.kommons.io.stream.IMemoryStream;
import io.karma.kommons.util.ExceptionUtils;
import io.karma.kommons.util.SystemInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.jar.Manifest;

/**
 * Main class for invoking a compilation, either programmatically
 * or via a command line interface through the given main entry point.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class ManganeseCompiler {
    private static final HashSet<String> IN_EXTENSIONS = new HashSet<>(Arrays.asList("ferrous", "fe"));
    private static final String OUT_EXTENSION = "fir";
    private static final ThreadLocal<ManganeseCompiler> INSTANCE = ThreadLocal.withInitial(ManganeseCompiler::new);
    private static boolean IS_STANDALONE = false;

    private final StringBuilder progressBuffer = new StringBuilder();
    private int numCompiledFiles;

    // @formatter:off
    private ManganeseCompiler() {}
    // @formatter:on

    public static @NotNull ManganeseCompiler getInstance() {
        return INSTANCE.get();
    }

    private static @NotNull Path deriveOutputPath(final @NotNull Path inPath) {
        final var inFile = inPath.toFile();

        if (inFile.isDirectory()) {
            return inPath; // If the input is a directory, we compile back into it.
        }

        final var inPathString = inPath.toString();
        final var lastSep = inPathString.lastIndexOf(File.separator);
        final var lastDot = inPathString.lastIndexOf('.');
        final var fileName = inPathString.substring(lastSep + 1, lastDot);
        return inPath.getParent().resolve(String.format("%s.fir", fileName));
    }

    // Main-entry point which invokes Manganese as a standalone tool
    @API(status = Status.INTERNAL)
    public static void main(final @NotNull String[] args) {
        Logger.INSTANCE.init(); // Initialize logger beforehand so we can catch all errors

        try {
            final var parser = new OptionParser("?iodv");
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.").withRequiredArg().ofType(String.class);
            parser.accepts("o", "A FIR file or a directory in which to save the compiled FIR bytecode.").withRequiredArg().ofType(String.class);
            parser.accepts("d", "Debug mode. This will print debug information during the compilation.");
            parser.accepts("v", "Prints version information about the compiler and runtime.");
            final var options = parser.parse(args);

            if (options.has("?")) {
                parser.printHelpOn(Logger.INSTANCE); // Print help
                return;
            }

            if (options.has("v")) {
                final var location = Objects.requireNonNull(ManganeseCompiler.class.getClassLoader().getResource("META-INF/MANIFEST.MF"));

                try (final var stream = location.openStream()) {
                    final var manifest = new Manifest(stream);
                    final var attribs = manifest.getMainAttributes();
                    Logger.INSTANCE.info("Manganese Version %s", attribs.getValue("Implementation-Version"));
                    Logger.INSTANCE.info("Running on %s", SystemInfo.getPlatformPair());
                }

                return;
            }

            // Update the log level if we are in verbose mode.
            if (options.has("d")) {
                Logger.INSTANCE.setLogLevel(LogLevel.DEBUG);
            }

            IS_STANDALONE = true;
            final var in = Paths.get((String) options.valueOf("i")).toAbsolutePath().normalize();

            // @formatter:off
            final var out = options.has("o")
                ? Paths.get((String)options.valueOf("o")).toAbsolutePath().normalize()
                : deriveOutputPath(in);
            // @formatter:on

            Logger.INSTANCE.info(getInstance().compile(in, out).getStatus().getFormattedMessage());
        }
        catch (OptionException e) {
            Logger.INSTANCE.info("Try running with -? to get some help!");
            System.exit(0);
        }
        catch (Throwable e) {
            ExceptionUtils.handleError(e, Logger.INSTANCE::fatal);
            System.exit(1);
        }
    }

    public static boolean isEmbedded() {
        return !IS_STANDALONE;
    }

    private static @NotNull List<Path> findCompilableFiles(final @NotNull Path path) {
        final var file = path.toFile();
        final var subFiles = file.listFiles();

        if (subFiles == null || subFiles.length == 0) {
            return Collections.emptyList();
        }

        final var files = new ArrayList<Path>();

        for (final var subFile : subFiles) {
            if (subFile.isDirectory()) {
                files.addAll(findCompilableFiles(subFile.toPath()));
                continue;
            }

            final var fileName = subFile.getName();

            if (!fileName.contains(".")) {
                continue; // Skip files without extension
            }

            final var lastDot = fileName.lastIndexOf(".");
            final var extension = fileName.substring(lastDot + 1);

            if (!IN_EXTENSIONS.contains(extension)) {
                continue; // Skip files without the right extension
            }

            files.add(subFile.toPath());
        }

        return files;
    }

    private @NotNull String getProgressIndicator(final int numFiles) {
        final var percent = (int) (((float) numCompiledFiles / (float) numFiles) * 100F);
        final var str = percent + "%%";
        final var length = str.length();

        progressBuffer.delete(0, progressBuffer.length());

        while (progressBuffer.length() < (5 - length)) {
            progressBuffer.append(' ');
        }

        progressBuffer.insert(0, '[');
        progressBuffer.append(str);
        progressBuffer.append(']');
        return progressBuffer.toString();
    }

    public @NotNull CompilationResult compile(final @NotNull Path in, final @NotNull Path out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded
        final var numFiles = in.toFile().isDirectory() ? findCompilableFiles(in).size() : 1;
        numCompiledFiles = 0;
        return compileRecursively(in, out, numFiles);
    }

    private @NotNull CompilationResult compileRecursively(final @NotNull Path in, final @NotNull Path out, final int numFiles) {
        final var inFile = in.toFile();
        final var outFile = out.toFile();

        if (inFile.isDirectory()) {
            if (!outFile.isDirectory()) {
                return new CompilationResult(CompilationStatus.IO_ERROR);
            }

            final var subFiles = findCompilableFiles(in);
            var result = new CompilationResult(CompilationStatus.SKIPPED);

            for (final var subFile : subFiles) {
                result = result.merge(compileRecursively(subFile, out, numFiles));
            }

            return result;
        }
        else {
            // @formatter:off
            Logger.INSTANCE.info(Ansi.ansi()
                .fg(Color.GREEN)
                .a(getProgressIndicator(numFiles))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(inFile.getAbsolutePath())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on

            numCompiledFiles++;
            return new CompilationResult(CompilationStatus.SUCCESS); // TODO
        }
    }

    public @NotNull CompilationStatus compile(final @NotNull IMemoryStream in, final @NotNull IMemoryStream out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded
        return CompilationStatus.SUCCESS;
    }
}

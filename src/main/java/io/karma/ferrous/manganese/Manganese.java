package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.io.stream.IMemoryStream;
import io.karma.kommons.io.stream.MemoryStream;
import io.karma.kommons.util.ExceptionUtils;
import io.karma.kommons.util.SystemInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
public final class Manganese implements ANTLRErrorListener {
    private static final HashSet<String> IN_EXTENSIONS = new HashSet<>(Arrays.asList("ferrous", "fe"));
    private static final String OUT_EXTENSION = "fir";
    private static final ThreadLocal<Manganese> INSTANCE = ThreadLocal.withInitial(Manganese::new);
    private static boolean IS_STANDALONE = false;

    private final StringBuilder progressBuffer = new StringBuilder();
    private int numCompiledFiles;
    private CompilationStatus status = CompilationStatus.SKIPPED;

    // @formatter:off
    private Manganese() {}
    // @formatter:on

    public static @NotNull Manganese getInstance() {
        return INSTANCE.get().reset();
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
                final var location = Objects.requireNonNull(Manganese.class.getClassLoader().getResource("META-INF/MANIFEST.MF"));

                try (final var stream = location.openStream()) {
                    final var manifest = new Manifest(stream);
                    final var attribs = manifest.getMainAttributes();
                    Logger.INSTANCE.printLogo();
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
            final var inFile = in.toFile();

            if (!inFile.exists()) {
                throw new IOException("Input file/directory does not exist");
            }

            // @formatter:off
            final var out = options.has("o")
                ? Paths.get((String)options.valueOf("o")).toAbsolutePath().normalize()
                : null;
            // @formatter:on

            Logger.INSTANCE.info(getInstance().compile(in, out).getStatus().getFormattedMessage());
        }
        catch (OptionException e) {
            Logger.INSTANCE.info("Try running with -? to get some help!");
            System.exit(0);
        }
        catch (IOException e) {
            Logger.INSTANCE.info(CompilationStatus.IO_ERROR.getFormattedMessage());
            System.exit(1);
        }
        catch (Throwable e) {
            ExceptionUtils.handleError(e, Logger.INSTANCE::fatal);
            System.exit(2);
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

    private @NotNull Manganese reset() {
        numCompiledFiles = 0;
        status = CompilationStatus.SKIPPED;
        return this;
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

    public void compile(final @NotNull Path in, final @Nullable Path out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded
        final var numFiles = in.toFile().isDirectory() ? findCompilableFiles(in).size() : 1;
        return compileRecursively(in, out, numFiles);
    }

    private void compileRecursively(final @NotNull Path in, @Nullable Path out, final int numFiles) {
        final var inFile = in.toFile();

        if (inFile.isDirectory()) {
            Logger.INSTANCE.debug("Traversing %s", in);

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

            // Attempt to deduce the output file name from the input file name, this may throw an AIOOB
            final var fileName = inFile.getName();
            final var lastDot = fileName.lastIndexOf('.');
            final var rawName = fileName.substring(0, lastDot);
            final var outFileName = String.format("%s.%s", rawName, OUT_EXTENSION);

            if (out == null) {
                // If the output path is null, derive it
                out = in.getParent().resolve(outFileName);
            }

            var outFile = out.toFile();

            if (!outFile.getName().contains(".")) { // Since isFile/isDirectory doesn't work reliably for non-existent files
                // If out is a directory, resolve the output file name
                out = out.resolve(outFileName);
                outFile = out.toFile();
            }

            final var outParentFile = outFile.getParentFile();

            if (!outParentFile.exists()) {
                if (outParentFile.mkdirs()) {
                    Logger.INSTANCE.debug("Created directory %s", outParentFile);
                }
            }

            var status = CompilationStatus.SKIPPED;

            Logger.INSTANCE.debug("I: %s", in);
            Logger.INSTANCE.debug("O: %s", out);

            try (final var fis = new FileInputStream(inFile)) {
                final var inStream = IMemoryStream.fromStream(MemoryStream::new, fis);

                try (final var fos = new FileOutputStream(outFile)) {
                    final var outStream = new MemoryStream(0);
                    status = status.worse(compile(inStream, outStream));
                    outStream.rewind();
                    outStream.writeTo(fos);
                }
            }
            catch (Throwable t) {
                ExceptionUtils.handleError(t, Logger.INSTANCE::error);
            }

            numCompiledFiles++;
            return new CompilationResult(status);
        }
    }

    public @NotNull CompilationStatus compile(final @NotNull IMemoryStream in, final @NotNull IMemoryStream out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            final var lexer = new FerrousLexer(charStream);

            final var tokenStream = new CommonTokenStream(lexer);
            final var parser = new FerrousParser(tokenStream);

            Logger.INSTANCE.debug(parser.eval().toStringTree());

            return CompilationStatus.SUCCESS;
        }
        catch (IOException t) {
            return CompilationStatus.IO_ERROR;
        }
        catch (Throwable t) {
            return CompilationStatus.UNKNOWN_ERROR;
        }
    }

    @Override
    public void syntaxError(final @NotNull Recognizer<?, ?> recognizer, final @NotNull Object offendingSymbol, final int line, final int charPositionInLine, final @NotNull String msg, final @NotNull RecognitionException e) {

    }

    @Override
    public void reportAmbiguity(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final boolean exact, final @NotNull BitSet ambigAlts, final @NotNull ATNConfigSet configs) {

    }

    @Override
    public void reportAttemptingFullContext(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final @NotNull BitSet conflictingAlts, final @NotNull ATNConfigSet configs) {

    }

    @Override
    public void reportContextSensitivity(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final int prediction, final @NotNull ATNConfigSet configs) {

    }
}

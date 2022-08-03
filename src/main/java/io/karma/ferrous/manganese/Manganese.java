package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.gen.BytecodeGenerator;
import io.karma.ferrous.manganese.gen.TranslationException;
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
import java.util.Optional;
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
    private final BytecodeGenerator generator = new BytecodeGenerator(this);
    private int numCompiledFiles;
    private CompilationStatus status = CompilationStatus.SKIPPED;
    private boolean tokenView = false;
    private boolean reportParserWarnings = false;
    private Path sourcePath;
    private Path outputPath;

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
        final var compiler = getInstance();
        var status = CompilationStatus.SKIPPED;

        try {
            if(args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser("?iodv");
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.").withRequiredArg().ofType(String.class);
            parser.accepts("o", "A FIR file or a directory in which to save the compiled FIR bytecode.").withRequiredArg().ofType(String.class);
            parser.accepts("d", "Debug mode. This will print debug information during the compilation.");
            parser.accepts("p", "Display parser warnings during compilation.").availableIf("d");
            parser.accepts("t", "Token view. This will print a tree structure containing all tokens during compilation.");
            parser.accepts("s", "Silent mode. This will suppress any warning level log messages during compilation.");
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

            if (options.has("s")) {
                Logger.INSTANCE.disableLogLevel(LogLevel.WARN);
            }

            if (options.has("t")) {
                compiler.setTokenView(true);
            }

            if (options.has("p")) {
                compiler.setReportParserWarnings(true);
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

            status = status.worse(compiler.compile(in, out).getStatus());
        }
        catch (OptionException | NoArgsException e) {
            // Special case; display help instead of logging the exception.
            Logger.INSTANCE.info("Try running with -? to get some help!");
            System.exit(0);
        }
        catch (IOException e) {
            status = status.worse(CompilationStatus.IO_ERROR);
        }
        catch (Throwable e) {
            ExceptionUtils.handleError(e, Logger.INSTANCE::fatal);
        }

        Logger.INSTANCE.info(status.getFormattedMessage());
        System.exit(status.getExitCode());
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

    private void resetCompilation() {
        status = CompilationStatus.SKIPPED;
        generator.reset();
        sourcePath = null;
        outputPath = null;
    }

    private @NotNull Manganese reset() {
        tokenView = false;
        return this;
    }

    public @NotNull CompilationStatus getStatus() {
        return status;
    }

    public @NotNull Optional<Path> getSourcePath() {
        return Optional.ofNullable(sourcePath);
    }

    public @NotNull Optional<Path> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public void setTokenView(final boolean tokenView) {
        this.tokenView = tokenView;
    }

    public void setReportParserWarnings(final boolean reportParserWarnings) {
        this.reportParserWarnings = reportParserWarnings;
    }

    public boolean isTokenViewEnabled() {
        return tokenView;
    }

    public boolean reportsParserWarnings() {
        return reportParserWarnings;
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

    public @NotNull CompilationResult compile(final @NotNull Path in, final @Nullable Path out) {
        Logger.INSTANCE.init(); // Initialize logger if we are embedded
        final var numFiles = in.toFile().isDirectory() ? findCompilableFiles(in).size() : 1;
        numCompiledFiles = 0; // Reset compilation counter
        return compileRecursively(in, out, numFiles);
    }

    private @NotNull CompilationResult compileRecursively(final @NotNull Path in, @Nullable Path out, final int numFiles) {
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

            Logger.INSTANCE.debug("Input: %s", in);
            Logger.INSTANCE.debug("Output: %s", out);

            sourcePath = in; // Update path fields
            outputPath = out;

            try (final var fis = new FileInputStream(inFile)) {
                final var inStream = IMemoryStream.fromStream(MemoryStream::new, fis);

                try (final var fos = new FileOutputStream(outFile)) {
                    final var outStream = new MemoryStream(0);
                    compile(inStream, outStream);
                    status = status.worse(this.status);
                    outStream.rewind();
                    outStream.writeTo(fos);
                }
            }
            catch (IOException e) {
                status = status.worse(CompilationStatus.IO_ERROR);
            }
            catch (Throwable e) {
                status = status.worse(CompilationStatus.UNKNOWN_ERROR);
                ExceptionUtils.handleError(e, Logger.INSTANCE::error);
            }

            numCompiledFiles++;
            return new CompilationResult(status);
        }
    }

    public void compile(final @NotNull IMemoryStream in, final @NotNull IMemoryStream out) {
        resetCompilation(); // Reset before each compilation
        Logger.INSTANCE.init(); // Initialize logger if we are embedded

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            final var lexer = new FerrousLexer(charStream);
            final var tokenStream = new CommonTokenStream(lexer);

            if (tokenView) {
                tokenStream.fill();
                final var numTokens = tokenStream.size();
                final var builder = Ansi.ansi();

                for (var i = 0; i < numTokens; i++) {
                    builder.reset();
                    Logger.INSTANCE.info(builder.fgBright(Color.MAGENTA).a(tokenStream.get(i)).a(Attribute.RESET).toString());
                }
            }

            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(this);
            final var evalContext = parser.eval();

            if (!status.isRecoverable()) {
                return;
            }

            status = status.worse(generator.generate(evalContext, out) ? CompilationStatus.SUCCESS : CompilationStatus.TRANSLATION_ERROR);
        }
        catch (TranslationException e) {
            status = status.worse(CompilationStatus.TRANSLATION_ERROR);
        }
        catch (IOException e) {
            status = status.worse(CompilationStatus.IO_ERROR);
        }
        catch (Throwable t) {
            status = status.worse(CompilationStatus.UNKNOWN_ERROR);
            ExceptionUtils.handleError(t, Logger.INSTANCE::error);
        }
    }

    @Override
    public void syntaxError(final @NotNull Recognizer<?, ?> recognizer, final @NotNull Object offendingSymbol, final int line, final int charPositionInLine, final @NotNull String msg, final @NotNull RecognitionException e) {
        status = status.worse(CompilationStatus.SYNTAX_ERROR);
        Logger.INSTANCE.error("Syntax error on line %d:%d: %s", line, charPositionInLine, msg);
    }

    @Override
    public void reportAmbiguity(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final boolean exact, final @NotNull BitSet ambigAlts, final @NotNull ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected ambiguity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportAttemptingFullContext(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final @NotNull BitSet conflictingAlts, final @NotNull ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected full context at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportContextSensitivity(final @NotNull Parser recognizer, final @NotNull DFA dfa, final int startIndex, final int stopIndex, final int prediction, final @NotNull ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected abnormally high context sensitivity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    private static final class NoArgsException extends RuntimeException {
        public NoArgsException() {
            super();
        }
    }
}

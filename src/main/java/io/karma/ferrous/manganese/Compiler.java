package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.translate.TranslationUnit;
import io.karma.ferrous.manganese.util.CompilationResult;
import io.karma.ferrous.manganese.util.CompilationStatus;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.SimpleFileVisitor;
import io.karma.ferrous.vanadium.FerrousLexer;
import io.karma.ferrous.vanadium.FerrousParser;
import io.karma.kommons.io.stream.MemoryStream;
import io.karma.kommons.io.stream.SimpleMemoryStream;
import io.karma.kommons.util.ExceptionUtils;
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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

/**
 * Main class for invoking a compilation, either programmatically
 * or via a command line interface through the given main entry point.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
public final class Compiler implements ANTLRErrorListener {
    private static final String[] IN_EXTENSIONS = {"ferrous", "fe"};
    private static final String OUT_EXTENSION = "bc";
    private static final ThreadLocal<Compiler> INSTANCE = ThreadLocal.withInitial(Compiler::new);

    private final StringBuilder progressBuffer = new StringBuilder();
    private CompilationStatus status = CompilationStatus.SKIPPED;
    private boolean tokenView = false;
    private boolean extendedTokenView = false;
    private boolean reportParserWarnings = false;
    private Path sourcePath;
    private Path outputPath;

    // @formatter:off
    private Compiler() {}
    // @formatter:on

    public static Compiler getInstance() {
        return INSTANCE.get().reset();
    }

    private static List<Path> findCompilableFiles(final Path path) {
        final var files = new ArrayList<Path>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor(filePath -> {
                final var fileName = filePath.getFileName().toString();
                for (final var ext : IN_EXTENSIONS) {
                    if (!fileName.endsWith(String.format(".%s", ext))) {
                        continue;
                    }
                    files.add(filePath);
                    break;
                }
                return FileVisitResult.CONTINUE;
            }));
        }
        catch (Exception error) { /* swallow exception */ }
        return files;
    }

    private void resetCompilation() {
        status = CompilationStatus.SKIPPED;
        sourcePath = null;
        outputPath = null;
    }

    private Compiler reset() {
        tokenView = false;
        extendedTokenView = false;
        return this;
    }

    public CompilationStatus getStatus() {
        return status;
    }

    public Optional<Path> getSourcePath() {
        return Optional.ofNullable(sourcePath);
    }

    public Optional<Path> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    public void setTokenView(final boolean tokenView, final boolean extendedTokenView) {
        this.tokenView = tokenView;
        this.extendedTokenView = extendedTokenView;
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

    private String getProgressIndicator(final int numFiles, final int index) {
        final var percent = (int) (((float) index / (float) numFiles) * 100F);
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

    public CompilationResult compile(final Path in, @Nullable Path out) {
        final var inputFiles = findCompilableFiles(in);
        final var numFiles = inputFiles.size();
        var status = CompilationStatus.SKIPPED;

        for (var i = 0; i < numFiles; ++i) {
            // @formatter:off
            Logger.INSTANCE.info(Ansi.ansi()
                .fg(Color.GREEN)
                .a(getProgressIndicator(numFiles, i))
                .a(Attribute.RESET)
                .a(" Compiling file ")
                .fg(Color.BLUE)
                .a(Attribute.INTENSITY_BOLD)
                .a(in.toAbsolutePath().toString())
                .a(Attribute.RESET)
                .toString());
            // @formatter:on

            // Attempt to deduce the output file name from the input file name, this may throw an AIOOB
            final var fileName = in.getFileName().toString();
            final var lastDot = fileName.lastIndexOf('.');
            final var rawName = fileName.substring(0, lastDot);
            final var outFileName = String.format("%s.%s", rawName, OUT_EXTENSION);

            if (out == null) {
                // If the output path is null, derive it
                out = in.getParent().resolve(outFileName);
            }
            if (!out.getFileName().toString().contains(".")) { // Since isFile/isDirectory doesn't work reliably for non-existent files
                // If out is a directory, resolve the output file name
                out = out.resolve(outFileName);
            }

            final var outParentFile = out.getParent();
            if (!Files.exists(outParentFile)) {
                try {
                    if (Files.createDirectories(outParentFile) != null) {
                        Logger.INSTANCE.debug("Created directory %s", outParentFile);
                    }
                }
                catch (Exception error) {
                    Logger.INSTANCE.error("Could not create directory at %s, skipping", outParentFile.toString());
                }
            }

            Logger.INSTANCE.debug("Input: %s", in);
            Logger.INSTANCE.debug("Output: %s", out);
            sourcePath = in; // Update path fields
            outputPath = out;

            try (final var fis = Files.newInputStream(in)) {
                try (final var fos = Files.newOutputStream(out)) {
                    final var outStream = new SimpleMemoryStream();
                    compile(fileName, MemoryStream.fromStream(SimpleMemoryStream::new, fis), outStream);
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
        }

        return new CompilationResult(status);
    }

    public void compile(final String name, final MemoryStream in, final MemoryStream out) {
        LLVMLoader.ensureNativesLoaded();
        resetCompilation(); // Reset before each compilation

        try {
            final var charStream = CharStreams.fromChannel(in, StandardCharsets.UTF_8);
            final var lexer = new FerrousLexer(charStream);
            var tokenStream = new CommonTokenStream(lexer);

            if (tokenView) {
                tokenStream.fill();
                final var tokens = tokenStream.getTokens();
                for (final var token : tokens) {
                    final var text = token.getText();
                    if (!extendedTokenView && text.isBlank()) {
                        continue; // Skip blank tokens if extended mode is disabled
                    }
                    // @formatter:off
                    final var tokenType = lexer.getTokenTypeMap()
                        .entrySet()
                        .stream()
                        .filter(e -> e.getValue().equals(token.getType()))
                        .findFirst()
                        .orElseThrow();
                    // @formatter:on
                    Logger.INSTANCE.info("%06d: %s (%s)", token.getTokenIndex(), text, tokenType);
                }
            }

            final var parser = new FerrousParser(tokenStream);
            parser.removeErrorListeners(); // Remove default error listener
            parser.addErrorListener(this);
            final var unit = new TranslationUnit(name);
            parser.addParseListener(unit);
            parser.file();

            if (!status.isRecoverable()) {
                return;
            }

            status = CompilationStatus.SUCCESS;
            unit.dispose();
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
    public void syntaxError(final Recognizer<?, ?> recognizer, final Object offendingSymbol, final int line,
                            final int charPositionInLine, final String msg, final RecognitionException e) {
        status = status.worse(CompilationStatus.SYNTAX_ERROR);
        Logger.INSTANCE.error("Syntax error on line %d:%d: %s", line, charPositionInLine, msg);
    }

    @Override
    public void reportAmbiguity(final Parser recognizer, final DFA dfa, final int startIndex, final int stopIndex,
                                final boolean exact, final BitSet ambigAlts, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected ambiguity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportAttemptingFullContext(final Parser recognizer, final DFA dfa, final int startIndex,
                                            final int stopIndex, final BitSet conflictingAlts,
                                            final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected full context at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }

    @Override
    public void reportContextSensitivity(final Parser recognizer, final DFA dfa, final int startIndex,
                                         final int stopIndex, final int prediction, final ATNConfigSet configs) {
        if (reportParserWarnings) {
            Logger.INSTANCE.debug("Detected abnormally high context sensitivity at %d:%d (%d)", startIndex, stopIndex, dfa.decision);
        }
    }
}

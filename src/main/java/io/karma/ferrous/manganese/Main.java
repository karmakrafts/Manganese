package io.karma.ferrous.manganese;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.kommons.util.SystemInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.jar.Manifest;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
public class Main {
    @API(status = Status.INTERNAL)
    public static void main(final String[] args) {
        final var compiler = Manganese.getInstance();
        var status = CompilationStatus.SKIPPED;

        try {
            if (args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser("?iodptTsv");
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.").withRequiredArg().ofType(String.class);
            parser.accepts("o", "An IL file or a directory in which to save the compiled IL blocks.").withRequiredArg().ofType(String.class);
            parser.accepts("d", "Debug mode. This will print debug information during the compilation.");
            parser.accepts("p", "Display parser warnings during compilation.").availableIf("d");
            parser.accepts("t", "Token view. This will print a tree structure containing all tokens during compilation.");
            parser.accepts("T", "Extended token view. This will print all whitespace characters.").availableIf("t");
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
                compiler.setTokenView(true, options.has("T"));
            }
            if (options.has("p")) {
                compiler.setReportParserWarnings(true);
            }

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
        catch (IOException error) {
            status = status.worse(CompilationStatus.IO_ERROR);
        }
        catch (Throwable error) {
            status = status.worse(CompilationStatus.UNKNOWN_ERROR);
        }

        Logger.INSTANCE.info(status.getFormattedMessage());
        System.exit(status.getExitCode());
    }

    private static final class NoArgsException extends RuntimeException {
        public NoArgsException() {
            super();
        }
    }
}

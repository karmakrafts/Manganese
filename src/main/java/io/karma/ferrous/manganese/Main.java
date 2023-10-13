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

import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.kommons.util.SystemInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.jar.Manifest;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
public final class Main {
    static {
        try {
            LLVMCore.getLibrary();
        }
        catch (UnsatisfiedLinkError e) { // @formatter:off
            throw new IllegalStateException("""
                Please configure the LLVM (13, 14 or 15) shared libraries path with:
                \t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or
                \t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>
            """, e);
        } // @formatter:on
    }

    @API(status = Status.INTERNAL)
    public static void main(final String[] args) {
        var status = CompileStatus.SKIPPED;

        try {
            if (args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser("?iodDptTsv");
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.").withRequiredArg().ofType(String.class);
            parser.accepts("o", "An IL file or a directory in which to save the compiled IL blocks.").withRequiredArg().ofType(String.class);
            parser.accepts("d", "Disassemble the output and print it into the console.");
            parser.accepts("D", "Debug mode. This will print debug information during the compilation.");
            parser.accepts("p", "Display parser warnings during compilation.").availableIf("d");
            parser.accepts("t", "Token view. This will print a tree structure containing all tokens during compilation.");
            parser.accepts("T", "Extended token view. This will print a tree view of all tokens.").availableIf("t");
            parser.accepts("s", "Silent mode. This will suppress any warning level log messages during compilation.");
            parser.accepts("v", "Prints version information about the compiler and runtime.");
            final var options = parser.parse(args);

            if (options.has("?")) {
                parser.printHelpOn(Logger.INSTANCE); // Print help
                return;
            }

            if (options.has("v")) {
                final var location = Objects.requireNonNull(Compiler.class.getClassLoader().getResource("META-INF/MANIFEST.MF"));
                try (final var stream = location.openStream()) {
                    final var manifest = new Manifest(stream);
                    final var attribs = manifest.getMainAttributes();
                    Logger.INSTANCE.printLogo();
                    Logger.INSTANCE.infoln("Manganese Version %s", attribs.getValue("Implementation-Version"));
                    Logger.INSTANCE.infoln("Running on %s", SystemInfo.getPlatformPair());
                }
                return;
            }

            // Update the log level if we are in verbose mode.
            if (options.has("D")) {
                Logger.INSTANCE.setLogLevel(LogLevel.DEBUG);
            }
            if (options.has("s")) {
                Logger.INSTANCE.disableLogLevel(LogLevel.WARN);
            }

            final var compiler = Compiler.getInstance();
            if (options.has("d")) {
                compiler.setDisassemble(true);
            }
            if (options.has("t")) {
                compiler.setTokenView(true, options.has("T"));
            }
            if (options.has("p")) {
                compiler.setReportParserWarnings(true);
            }

            final var in = Paths.get((String) options.valueOf("i")).toAbsolutePath().normalize();
            if (!Files.exists(in)) {
                throw new IOException("Input file/directory does not exist");
            }

            // @formatter:off
            final var out = options.has("o")
                ? Paths.get((String)options.valueOf("o")).toAbsolutePath().normalize()
                : null;
            // @formatter:on

            final var result = compiler.compile(in, out);
            status = status.worse(result.getStatus());

            final var errors = result.getErrors();
            Collections.sort(errors);
            final var lines = new HashSet<String>();

            for (final var error : errors) {
                final var text = error.getText();
                if (lines.contains(text)) {
                    continue;
                }
                error.print(System.out);
                lines.add(text);
            }
        }
        catch (OptionException | NoArgsException e) {
            // Special case; display help instead of logging the exception.
            Logger.INSTANCE.infoln("Try running with -? to get some help!");
            System.exit(0);
        }
        catch (IOException error) {
            status = status.worse(CompileStatus.IO_ERROR);
        }
        catch (Throwable error) {
            status = status.worse(CompileStatus.UNKNOWN_ERROR);
        }

        Logger.INSTANCE.infoln(status.getFormattedMessage());
        System.exit(status.getExitCode());
    }

    private static final class NoArgsException extends RuntimeException {
        public NoArgsException() {
            super();
        }
    }
}

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

import io.karma.ferrous.manganese.target.CodeModel;
import io.karma.ferrous.manganese.target.OptimizationLevel;
import io.karma.ferrous.manganese.target.Relocation;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.kommons.util.SystemInfo;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;
import java.util.jar.Manifest;

import static org.lwjgl.llvm.LLVMCore.LLVMContextSetOpaquePointers;
import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
@API(status = Status.INTERNAL)
public final class Main {
    @API(status = Status.INTERNAL)
    public static void main(final String[] args) {
        var status = CompileStatus.SKIPPED;

        try {
            if (args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser("?iodDpPtTsvVbBm");
            // @formatter:off
            parser.accepts("?", "Print this help dialog");
            parser.accepts("i", "A Ferrous file or directory of files from which to compile.")
                .withRequiredArg()
                .ofType(String.class);
            parser.accepts("o", "The path to the linked binary.")
                .withRequiredArg()
                .ofType(String.class);
            parser.accepts("d", "Disassemble the output and print it into the console.");
            parser.accepts("D", "Debug mode. This will print debug information during the compilation.");
            parser.accepts("p", "Display parser warnings during compilation.")
                .availableIf("d");
            parser.accepts("P", "Disable opaque pointers. Useful for disassembling compile output.");
            parser.accepts("t", "The target triple for which to compile the code.");
            parser.accepts("T", "Token view. This will print a tree structure containing all tokens during compilation.");
            parser.accepts("s", "Silent mode. This will suppress any warning level log messages during compilation.");
            parser.accepts("v", "Prints version information about the compiler and runtime.");
            parser.accepts("V", "Enable verbose errors. This will likely show some garbage.");
            parser.accepts("b", "The path to the build directory used for storing compiled IL objects.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("build");
            parser.accepts("B", "Dump bitcode to files while compiling.");
            parser.accepts("m", "The name of the output module, will default to the name of the first input file.");
            // @formatter:on
            final var options = parser.parse(args);

            if (options.has("?")) {
                parser.printHelpOn(Logger.INSTANCE); // Print help
                return;
            }

            if (options.has("v")) {
                final var location = Objects.requireNonNull(
                        Compiler.class.getClassLoader().getResource("META-INF/MANIFEST.MF"));
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
            if (options.has("P")) {
                LLVMContextSetOpaquePointers(LLVMGetGlobalContext(), false);
            }

            final var compiler = new Compiler(Target.getHostTarget(), "", OptimizationLevel.DEFAULT, Relocation.DEFAULT,
                                              CodeModel.DEFAULT);
            compiler.setDisassemble(options.has("d"));
            compiler.setTokenView(options.has("T"), false);
            compiler.setReportParserWarnings(options.has("p"));
            compiler.setVerbose(options.has("V"));
            compiler.setSaveBitcode(options.has("B"));

            final var in = Paths.get((String) options.valueOf("i")).toAbsolutePath().normalize();
            if (!Files.exists(in)) {
                compiler.dispose();
                throw new IOException("Input file/directory does not exist");
            }

            // @formatter:off
            final var out = options.has("o")
                ? Paths.get((String)options.valueOf("o")).toAbsolutePath().normalize()
                : null;
            // @formatter:on

            final var result = compiler.compile(in, out);
            compiler.dispose();
            status = status.worse(result.status());

            final var errors = result.errors();
            Collections.sort(errors);
            errors.forEach(error -> error.print(System.out));
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

        Logger.INSTANCE.infoln("%s", status.getFormattedMessage());
        System.exit(status.getExitCode());
    }

    private static final class NoArgsException extends RuntimeException {
        public NoArgsException() {
            super();
        }
    }
}

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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileStatus;
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.target.*;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.ferrous.manganese.util.Utils;
import io.karma.kommons.util.SystemInfo;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.jar.Manifest;

import static org.lwjgl.llvm.LLVMTargetMachine.LLVMGetHostCPUFeatures;

/**
 * @author Alexander Hinze
 * @since 11/10/2023
 */
@API(status = Status.INTERNAL)
final class Main {
    public static void main(final String[] args) {
        Manganese.init();
        var status = CompileStatus.SUCCESS;

        try {
            if (args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser(true);
            // @formatter:off
            final var helpOpt = parser.accepts("?", "Print this help dialog or list valid values for an enum option.");
            final var inOpt = parser.accepts("i", "A Ferrous file or directory of files from which to compile.")
                .withRequiredArg()
                .ofType(String.class);
            final var outOpt = parser.accepts("o", "The path to the linked binary.")
                .withOptionalArg()
                .ofType(String.class);
            final var optimizationOpt = parser.accepts("O", "The level of optimization.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(OptimizationLevel.DEFAULT.getName());
            final var threadsOpt = parser.accepts("j", "Number of threads to use when compiling.")
                .withOptionalArg()
                .ofType(Integer.class)
                .defaultsTo(Runtime.getRuntime().availableProcessors());
            final var disassembleOpt = parser.accepts("d", "Disassemble the output and print it into the console.");
            final var debugOpt = parser.accepts("D", "Debug mode. This will print debug information during the compilation.");
            final var parseWarningsOpt = parser.accepts("p", "Display parser warnings during compilation.")
                .availableIf("D");
            final var opaquePointerOpt = parser.accepts("P", "Disable opaque pointers. Useful for disassembling compile output.");
            final var targetOpt = parser.accepts("t", "The target triple for which to compile the code.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Target.getHostTargetTriple());
            final var tokenViewOpt = parser.accepts("T", "Token view. This will print a tree structure containing all tokens during compilation.");
            final var silentOpt = parser.accepts("s", "Silent mode. This will suppress any warning level log messages during compilation.");
            final var versionOpt = parser.accepts("v", "Prints version information about the compiler and runtime.");
            final var codeModelOpt = parser.accepts("M", "The code model to use when generating assembly code for the target machine.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(CodeModel.DEFAULT.getName());
            final var featuresOpt = parser.accepts("f", "Feature options forwarded to LLVM during compilation.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Objects.requireNonNull(LLVMGetHostCPUFeatures()));
            final var fileTypeOpt = parser.accepts("F", "The type of the output file.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(FileType.OBJECT.getExtension());
            final var relocOpt = parser.accepts("r", "The type of relocation used when linking the final object.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Relocation.DEFAULT.getName());
            final var cpuOpt = parser.accepts("c", "The type of processor to compile for. Depends on the given target architecture.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("");
            // @formatter:on
            final var options = parser.parse(args);

            if (options.has(helpOpt)) {
                if (options.has(optimizationOpt)) {
                    Logger.INSTANCE.infoln("Available optimization levels:");
                    for (final var level : OptimizationLevel.values()) {
                        Logger.INSTANCE.infoln("  - %s", level.getName());
                    }
                    return;
                }
                if (options.has(fileTypeOpt)) {
                    Logger.INSTANCE.infoln("Available file types:");
                    for (final var type : FileType.values()) {
                        Logger.INSTANCE.infoln("  - %s", type.getExtension());
                    }
                    return;
                }
                if (options.has(codeModelOpt)) {
                    Logger.INSTANCE.infoln("Available code models:");
                    for (final var model : CodeModel.values()) {
                        Logger.INSTANCE.infoln("  - %s", model.getName());
                    }
                    return;
                }
                if (options.has(relocOpt)) {
                    Logger.INSTANCE.infoln("Available relocation types:");
                    for (final var type : Relocation.values()) {
                        Logger.INSTANCE.infoln("  - %s", type.getName());
                    }
                    return;
                }
                parser.formatHelpWith(new BuiltinHelpFormatter(120, 8));
                parser.printHelpOn(Logger.INSTANCE); // Print help
                Logger.INSTANCE.infoln("You can also combine the following options with -?:");
                Logger.INSTANCE.infoln("  -O, -F, -M, -r");
                return;
            }
            if (options.has(versionOpt)) {
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

            final var targetTriple = options.valueOf(targetOpt);
            final var target = Manganese.createTarget(targetTriple);

            final var features = options.has(featuresOpt) ? options.valueOf(featuresOpt) : "";
            final var optLevel = OptimizationLevel.byName(options.valueOf(optimizationOpt));
            final var relocation = Relocation.byName(options.valueOf(relocOpt));
            final var codeModel = CodeModel.byName(options.valueOf(codeModelOpt));
            final var fileType = FileType.byExtension(options.valueOf(fileTypeOpt));
            if (optLevel.isEmpty() || relocation.isEmpty() || codeModel.isEmpty() || fileType.isEmpty()) {
                Logger.INSTANCE.error("Malformed parameter");
                return;
            }

            final var targetMachine = Manganese.createTargetMachine(target, features, optLevel.get(), relocation.get(),
                    codeModel.get(), options.valueOf(cpuOpt));
            final var compiler = Manganese.createCompiler(targetMachine, options.valueOf(threadsOpt));
            compiler.setDisassemble(options.has(disassembleOpt));
            compiler.setTokenView(options.has(tokenViewOpt), false);
            compiler.setReportParserWarnings(options.has(parseWarningsOpt));
            compiler.setEnableOpaquePointers(!options.has(opaquePointerOpt));

            // Update the log level if we are in verbose mode.
            if (options.has(debugOpt)) {
                Logger.INSTANCE.setLogLevel(LogLevel.DEBUG);
            }
            if (options.has(silentOpt)) {
                for (final var level : LogLevel.values()) {
                    Logger.INSTANCE.disableLogLevel(level);
                }
            }

            final var in = Path.of(options.valueOf(inOpt));
            // @formatter:off
            final var out = options.has(outOpt)
                ? Path.of(options.valueOf(outOpt))
                : Path.of(String.format("%s.%s", Utils.getRawFileName(in), fileType.get().getExtension()));
            // @formatter:on

            final var context = new CompileContext();
            final var result = compiler.compile(in, out, fileType.get(), context);
            context.dispose();
            targetMachine.dispose();
            status = status.worse(result.status());

            final var errors = result.errors();
            Collections.sort(errors);
            errors.forEach(error -> error.print(System.out));
        } catch (OptionException | NoArgsException error) {
            // Special case; display help instead of logging the exception.
            Logger.INSTANCE.infoln("Try running with -? to get some help!");
            System.exit(0);
        } catch (IOException error) {
            Logger.INSTANCE.errorln("%s", error.toString());
            status = status.worse(CompileStatus.IO_ERROR);
        } catch (Throwable error) {
            Logger.INSTANCE.errorln("%s", error.toString());
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

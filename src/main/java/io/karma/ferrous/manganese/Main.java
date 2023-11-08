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
import io.karma.ferrous.manganese.linker.LinkModel;
import io.karma.ferrous.manganese.linker.LinkTargetType;
import io.karma.ferrous.manganese.linker.LinkerType;
import io.karma.ferrous.manganese.profiler.Profiler;
import io.karma.ferrous.manganese.target.*;
import io.karma.ferrous.manganese.util.KitchenSink;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Logger.LogLevel;
import io.karma.kommons.util.SystemInfo;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
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
    private static <E extends Enum<E>> void printAvailableValues(final Class<E> type, final String message) {
        Logger.INSTANCE.infoln(message);
        final var values = type.getEnumConstants();
        for (final var value : values) {
            Logger.INSTANCE.infoln("  - %s", value);
        }
    }

    public static void main(final String[] args) {
        Manganese.init();
        var status = CompileStatus.SUCCESS;

        try {
            if (args.length == 0) {
                throw new NoArgsException();
            }

            final var parser = new OptionParser(false);
            // @formatter:off
            // Actions
            final var helpOpt = parser.accepts("?", "Print this help dialog or list valid values for an enum option.");
            final var versionOpt = parser.accepts("v", "Prints version information about the compiler and runtime.");
            // IO options
            final var inOpt = parser.accepts("i", "A Ferrous file or directory of files from which to compile.")
                .withRequiredArg()
                .ofType(String.class);
            final var outOpt = parser.accepts("o", "The path to the linked binary.")
                .withOptionalArg()
                .ofType(String.class);
            // General options
            final var silentOpt = parser.accepts("s", "Silent mode. This will suppress any warning level log messages during compilation.");
            final var threadsOpt = parser.accepts("j", "Number of threads to use when compiling.")
                .withOptionalArg()
                .ofType(Integer.class)
                .defaultsTo(Runtime.getRuntime().availableProcessors());
            // Debug options
            final var debugOpt = parser.accepts("d", "Debug mode. This will print debug information during the compilation.");
            final var parseWarningsOpt = parser.accepts("Dp", "Display parser warnings during compilation.")
                .availableIf("d");
            final var tokenViewOpt = parser.accepts("Dt", "Token view. This will print a tree structure containing all tokens during compilation.")
                .availableIf("d");
            final var opaquePointerOpt = parser.accepts("DP", "Disable opaque pointers. Useful for disassembling compile output.")
                .availableIf("d");
            final var disassembleOpt = parser.accepts("Dd", "Disassemble the output and print it into the console.")
                .availableIf("d");
            final var profilerOpt = parser.accepts("Dx", "Enable the profiler and show results at the end of the compilation.")
                .availableIf("d");
            // Target options
            final var targetOpt = parser.accepts("t", Ansi.ansi()
                    .a("The target triple to compile for. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Target.getHostTargetTriple());
            final var codeModelOpt = parser.accepts("Tm", Ansi.ansi()
                    .a("The code model to use when generating assembly code for the target machine. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(CodeModel.DEFAULT.getName());
            final var featuresOpt = parser.accepts("Tf", "Feature options forwarded to LLVM during compilation.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Objects.requireNonNull(LLVMGetHostCPUFeatures()));
            final var relocOpt = parser.accepts("Tr", Ansi.ansi()
                    .a("The type of code relocation used by the linker. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Relocation.DYN_NO_PIC.getName());
            final var cpuOpt = parser.accepts("Tc", "The type of processor to compile for. Depends on the given target architecture.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("");
            final var optimizationOpt = parser.accepts("To", Ansi.ansi()
                    .a("The level of optimization applied to the resulting assembly code. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(OptimizationLevel.DEFAULT.getName());
            // Link options
            final var linkerTypeOpt = parser.accepts("LT", Ansi.ansi()
                    .a("The type of linker to use when creating the target binary. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(Platform.getHostPlatform().getDefaultLinkerType().getName());
            final var linkModelOpt = parser.accepts("Lm", Ansi.ansi()
                    .a("The type of link model to use while linking. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(LinkModel.FULL.getName());
            final var linkTargetTypeOpt = parser.accepts("Lt", Ansi.ansi()
                    .a("The type of target to create when linking. ")
                    .fg(Ansi.Color.CYAN)
                    .a("List with ?")
                    .a(Ansi.Attribute.RESET)
                    .toString())
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo(LinkTargetType.EXECUTABLE.getName());
            final var linkerOptionsOpt = parser.accepts("Lo", "Options passed directly to the linker.")
                .withOptionalArg()
                .ofType(String.class)
                .defaultsTo("");
            // @formatter:on
            final var options = parser.parse(args);

            if (options.has(helpOpt)) {
                if (options.has(featuresOpt)) {
                    Logger.INSTANCE.infoln("Available CPU features:");
                    // @formatter:off
                    Arrays.stream(Objects.requireNonNull(LLVMGetHostCPUFeatures()).split(","))
                        .filter(feat -> feat.startsWith("+"))
                        .map(feat -> feat.substring(1))
                        .forEach(feat -> Logger.INSTANCE.infoln("  - %s", feat));
                    // @formatter:on
                    return;
                }
                if (options.has(targetOpt)) {
                    printAvailableValues(Architecture.class, "Available architectures:");
                    printAvailableValues(Platform.class, "Available platforms:");
                    printAvailableValues(ABI.class, "Available ABIs:");
                    return;
                }
                if (options.has(linkTargetTypeOpt)) {
                    printAvailableValues(LinkTargetType.class, "Available link target types:");
                    return;
                }
                if (options.has(linkModelOpt)) {
                    printAvailableValues(LinkModel.class, "Available link models:");
                    return;
                }
                if (options.has(linkerTypeOpt)) {
                    printAvailableValues(LinkerType.class, "Available linker types:");
                    return;
                }
                if (options.has(optimizationOpt)) {
                    printAvailableValues(OptimizationLevel.class, "Available optimization levels:");
                    return;
                }
                if (options.has(codeModelOpt)) {
                    printAvailableValues(CodeModel.class, "Available code models:");
                    return;
                }
                if (options.has(relocOpt)) {
                    printAvailableValues(Relocation.class, "Available relocation types:");
                    return;
                }
                parser.formatHelpWith(new BuiltinHelpFormatter(120, 8));
                parser.printHelpOn(Logger.INSTANCE); // Print help
                return;
            }
            if (options.has(versionOpt)) {
                final var location = Objects.requireNonNull(Compiler.class.getClassLoader().getResource(
                    "META-INF/MANIFEST.MF"));
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
            final var linkModel = LinkModel.byName(options.valueOf(linkModelOpt));
            final var linkTargetType = LinkTargetType.byName(options.valueOf(linkTargetTypeOpt));
            if (optLevel.isEmpty() || relocation.isEmpty() || codeModel.isEmpty() || linkModel.isEmpty() || linkTargetType.isEmpty()) {
                Logger.INSTANCE.errorln("Malformed parameter");
                return;
            }

            final var targetMachine = Manganese.createTargetMachine(target,
                features,
                optLevel.get(),
                relocation.get(),
                codeModel.get(),
                options.valueOf(cpuOpt));
            final var linkerType = LinkerType.byName(options.valueOf(linkerTypeOpt));
            if (linkerType.isEmpty()) {
                Logger.INSTANCE.errorln("Malformed parameter");
                return;
            }

            final var linker = linkerType.get().create();
            linker.addRawOptions(options.valueOf(linkerOptionsOpt));

            final var compiler = Manganese.createCompiler(targetMachine, linker, options.valueOf(threadsOpt));
            compiler.setDisassemble(options.has(disassembleOpt));
            compiler.setTokenView(options.has(tokenViewOpt), false);
            compiler.setReportParserWarnings(options.has(parseWarningsOpt));
            compiler.setEnableOpaquePointers(!options.has(opaquePointerOpt));

            // Update the log level if we are in verbose mode.
            final var debugMode = options.has(debugOpt);
            if (debugMode) {
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
                : Path.of(KitchenSink.getRawFileName(in));
            // @formatter:on

            final var context = new CompileContext();
            final var result = compiler.compile(in, out, context, linkModel.get(), linkTargetType.get());
            context.dispose();
            targetMachine.dispose();
            status = status.worse(result.status());

            final var errors = result.errors();
            Collections.sort(errors);
            errors.forEach(error -> error.print(System.out));

            if (options.has(profilerOpt)) {
                System.out.printf("\n%s\n", Profiler.INSTANCE.renderSections());
            }
        }
        catch (OptionException | NoArgsException error) {
            // Special case; display help instead of logging the exception.
            Logger.INSTANCE.infoln("Try running with -? to get some help!");
            System.exit(0);
        }
        catch (IOException error) {
            Logger.INSTANCE.errorln("%s", error.toString());
            status = status.worse(CompileStatus.IO_ERROR);
        }
        catch (Throwable error) {
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

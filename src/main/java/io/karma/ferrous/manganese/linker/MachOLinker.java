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

package io.karma.ferrous.manganese.linker;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.compiler.CompileErrorCode;
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.Platform;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.INTERNAL)
public final class MachOLinker extends AbstractLinker {
    MachOLinker() {
        super(EnumSet.of(Architecture.PPC32,
            Architecture.PPC64,
            Architecture.X86,
            Architecture.X86_64,
            Architecture.AARCH64));
    }

    private static String remapArchitectureName(final Target target) {
        return switch(target.getArchitecture()) { // @formatter:off
            case PPC32   -> "ppc";
            case PPC64   -> "ppc64";
            case X86     -> "i386";
            case X86_64  -> "x86_64";
            case AARCH64 -> "aarch64";
            default      -> throw new IllegalArgumentException("Unsupported architecture");
        }; // @formatter:on
    }

    private static void handleLibraries(final ArrayList<String> buffer, final LinkModel linkModel,
                                        final String version) {
        if (linkModel == LinkModel.FULL) {
            buffer.add("-L/usr/lib");
            buffer.add("-L/usr/lib/system");
            final var major = Integer.parseInt(version.substring(0, version.indexOf('.')));
            if (major >= 12) {
                buffer.add("-lsystem_kernel");
                buffer.add("-lsystem_platform");
                buffer.add("-lsystem_pthread");
            }
            else {
                buffer.add("-lSystem");
            }
        }
    }

    @Override
    protected void buildCommand(final ArrayList<String> buffer, final String command, final Path outFile,
                                final Path objectFile, final LinkModel linkModel, final TargetMachine targetMachine,
                                final CompileContext compileContext, final LinkTargetType targetType) {
        final var target = targetMachine.getTarget();
        if (linkModel == LinkModel.FULL && target.getPlatform() != Platform.MACOS) {
            compileContext.reportError("Full link model not supported when cross-compiling", CompileErrorCode.E5005);
            return;
        }
        buffer.add(command);
        buffer.add("-arch");
        buffer.add(remapArchitectureName(target));
        buffer.add("-platform_version");
        buffer.add(target.getPlatform().getName());
        final var osVersion = System.getProperty("os.version");
        buffer.add(osVersion);
        buffer.add(osVersion);
        handleLibraries(buffer, linkModel, osVersion);
        buffer.addAll(options);
        buffer.add("-o");
        buffer.add(outFile.toAbsolutePath().normalize().toString());
        buffer.add(objectFile.toAbsolutePath().normalize().toString());
    }

    @Override
    public LinkerType getType() {
        return LinkerType.MACHO;
    }
}

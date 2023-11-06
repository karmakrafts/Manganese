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
import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Logger;
import io.karma.ferrous.manganese.util.Utils;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.INTERNAL)
public abstract class AbstractLinker implements Linker {
    protected final EnumSet<Architecture> supportedArchitectures;
    protected final ArrayList<String> options = new ArrayList<>();
    protected Path dynamicLinkerPath;

    protected AbstractLinker(final EnumSet<Architecture> supportedArchitectures) {
        this.supportedArchitectures = supportedArchitectures;
    }

    protected abstract void buildCommand(final ArrayList<String> buffer, final String command, final Path outFile,
                                         final Path objectFile, final LinkModel linkModel,
                                         final TargetMachine targetMachine, final CompileContext compileContext,
                                         final LinkTargetType targetType);

    @Override
    public void link(final Compiler compiler, final CompileContext compileContext, final Path outFile,
                     final Path objectFile, final LinkModel linkModel, final TargetMachine targetMachine,
                     final LinkTargetType targetType) {
        if (!supportedArchitectures.contains(compiler.getTargetMachine().getTarget().getArchitecture())) {
            compileContext.reportError(CompileErrorCode.E5004);
            return;
        }
        final var command = getType().findCommand();
        if (command == null) {
            compileContext.reportError(CompileErrorCode.E5000);
            return;
        }
        try {
            final var commandBuffer = new ArrayList<String>();
            buildCommand(commandBuffer,
                command,
                outFile,
                objectFile,
                linkModel,
                targetMachine,
                compileContext,
                targetType);
            final var process = Utils.createProcess(commandBuffer.toArray(String[]::new)).start();
            try (final var reader = process.inputReader()) {
                while (process.isAlive()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Logger.INSTANCE.infoln(line);
                    }
                }
            }
            if (process.waitFor() != 0) {
                try (final var reader = process.errorReader()) {
                    final var error = Utils.makeCompilerMessage(reader.lines().collect(Collectors.joining("\n")));
                    compileContext.reportError(error, CompileErrorCode.E5002);
                }
            }
        }
        catch (IOException error) {
            compileContext.reportError(Utils.makeCompilerMessage(error.getMessage()), CompileErrorCode.E5001);
        }
        catch (InterruptedException error) {
            compileContext.reportError(Utils.makeCompilerMessage(error.getMessage()), CompileErrorCode.E5003);
        }
    }

    @Override
    public @Nullable Path getDynamicLinkerPath() {
        return dynamicLinkerPath;
    }

    @Override
    public void setDynamicLinkerPath(final Path path) {
        dynamicLinkerPath = path;
    }

    @Override
    public void addOption(final String option) {
        if (options.contains(option)) {
            return;
        }
        options.add(option);
    }

    @Override
    public void clearOptions() {
        options.clear();
    }

    @Override
    public EnumSet<Architecture> getSupportedArchitectures() {
        return supportedArchitectures;
    }
}

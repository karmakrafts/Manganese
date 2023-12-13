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

import io.karma.ferrous.manganese.compiler.Compiler;
import io.karma.ferrous.manganese.linker.Linker;
import io.karma.ferrous.manganese.llvm.LLVMUtils;
import io.karma.ferrous.manganese.target.*;
import io.karma.ferrous.manganese.util.DiagnosticSeverity;
import io.karma.ferrous.manganese.util.Logger;
import org.apiguardian.api.API;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.llvm.LLVMDiagnosticHandler;
import org.lwjgl.system.MemoryUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.llvm.LLVMInitialization.LLVMInitializeCore;
import static org.lwjgl.llvm.LLVMInitialization.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = API.Status.STABLE)
public final class Manganese {
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);
    private static final AtomicReference<String> LAST_LLVM_ERROR = new AtomicReference<>("");

    // @formatter:off
    private Manganese() {}
    // @formatter:on

    @API(status = API.Status.INTERNAL)
    public static String getLastLLVMError() {
        return LAST_LLVM_ERROR.getAndSet("");
    }

    public static void init() {
        if (IS_INITIALIZED.getAndSet(true)) {
            return;
        }

        LLVMUtils.loadLLVM();
        LLVMUtils.checkNatives();
        final var registry = LLVMCore.LLVMGetGlobalPassRegistry();
        if (registry == NULL) {
            throw new IllegalStateException("Could not retrieve global pass registry");
        }

        LLVMInitializeCore(registry);
        LLVMInitializeTarget(registry);
        LLVMInitializeAnalysis(registry);
        LLVMInitializeCodeGen(registry);
        LLVMInitializeVectorization(registry);

        LLVMUtils.initX86();
        LLVMUtils.initMips();
        LLVMUtils.initRISCV();
        LLVMUtils.initPowerPC();
        LLVMUtils.initARM();
        LLVMUtils.initAArch64();
        LLVMUtils.initAVR();
        LLVMUtils.initWebAssembly();

        LLVMContextSetDiagnosticHandler(LLVMGetGlobalContext(), LLVMDiagnosticHandler.create((info, ctx) -> {
            final var severityValue = LLVMGetDiagInfoSeverity(info);
            final var severity = DiagnosticSeverity.byValue(severityValue);
            if (severity.isEmpty()) {
                Logger.INSTANCE.errorln(STR."Unknown diagnostic severity \{severityValue}");
                return;
            }
            if (severity.get() != DiagnosticSeverity.ERROR) {
                return;
            }
            final var message = MemoryUtil.memUTF8(LLVMGetDiagInfoDescription(info)).toString();
            LAST_LLVM_ERROR.set(message);
        }), NULL);
    }

    public static Target createTarget(final Architecture arch, final Platform platform, final ABI abi) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return new Target(arch, platform, abi);
    }

    public static Target createTarget(final String triple) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return Target.parse(triple).orElseThrow();
    }

    public static TargetMachine createTargetMachine(final Target target, final String features,
                                                    final OptimizationLevel level, final Relocation reloc,
                                                    final CodeModel model, final String cpu) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return new TargetMachine(target, features, level, reloc, model, cpu);
    }

    public static Compiler createCompiler(final TargetMachine machine, final Linker linker, final int numThreads,
                                          final boolean enableProfiler) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return new Compiler(machine, linker, numThreads, enableProfiler);
    }

    public static Compiler createCompiler() {
        final var numThreads = Runtime.getRuntime().availableProcessors();
        final var cpu = Architecture.getHostArchitecture().getDefaultCPU();
        final var target = Target.getHostTarget();
        final var linker = target.getPlatform().getDefaultLinkerType().create();
        return createCompiler(new TargetMachine(target,
            "",
            OptimizationLevel.DEFAULT,
            Relocation.DEFAULT,
            CodeModel.DEFAULT,
            cpu), linker, numThreads, false);
    }
}

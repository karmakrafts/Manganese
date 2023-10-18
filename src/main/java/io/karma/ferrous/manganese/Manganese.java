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

import io.karma.ferrous.manganese.target.ABI;
import io.karma.ferrous.manganese.target.Architecture;
import io.karma.ferrous.manganese.target.CodeModel;
import io.karma.ferrous.manganese.target.FileType;
import io.karma.ferrous.manganese.target.OptimizationLevel;
import io.karma.ferrous.manganese.target.Platform;
import io.karma.ferrous.manganese.target.Relocation;
import io.karma.ferrous.manganese.target.Target;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.LLVMUtils;
import org.lwjgl.llvm.LLVMCore;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.llvm.LLVMInitialization.*;
import static org.lwjgl.llvm.LLVMTargetX86.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
public final class Manganese {
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    // @formatter:off
    private Manganese() {}
    // @formatter:on

    public static void init() {
        if (IS_INITIALIZED.getAndSet(true)) {
            return;
        }

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

        LLVMInitializeX86Target();
        LLVMInitializeX86TargetInfo();
        LLVMInitializeX86TargetMC();
        LLVMInitializeX86AsmParser();
        LLVMInitializeX86AsmPrinter();
        LLVMInitializeX86Disassembler();
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
                                                    final CodeModel model, final FileType fileType) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return new TargetMachine(target, features, level, reloc, model, fileType);
    }

    public static Compiler createCompiler(final TargetMachine machine) {
        if (!IS_INITIALIZED.get()) {
            throw new IllegalStateException("Not initialized");
        }
        return new Compiler(machine);
    }
}

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

package io.karma.ferrous.manganese.llvm;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.llvm.LLVMCore;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.karma.ferrous.manganese.llvm.LLVMTargetAArch64.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetARM.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetAVR.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetMips.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetPowerPC.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetRISCV.*;
import static io.karma.ferrous.manganese.llvm.LLVMTargetWebAssembly.*;
import static org.lwjgl.llvm.LLVMCore.nLLVMDisposeMessage;
import static org.lwjgl.llvm.LLVMTargetX86.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.INTERNAL)
public final class LLVMUtils {
    private static final String[] PATHS_TO_SEARCH = {"/usr/share/lib", "/usr/lib", "/Library", "/"};
    private static final String[] NAMES_TO_SEARCH = {"llvm-15/build/Release", "llvm-14/build/Release", "llvm-13/build/Release", "llvm-12/build/Release", "llvm-11/build/Release", "llvm/build/Release", "LLVM/build/Release", "llvm-15", "llvm-14", "llvm-13", "llvm-12", "llvm-11", "llvm", "LLVM"};

    // @formatter:off
    private LLVMUtils() {}
    // @formatter:on

    public static void initMips() {
        LLVMInitializeMipsTarget();
        LLVMInitializeMipsTargetInfo();
        LLVMInitializeMipsTargetMC();
        LLVMInitializeMipsAsmParser();
        LLVMInitializeMipsAsmPrinter();
        LLVMInitializeMipsDisassembler();
    }

    public static void initRISCV() {
        LLVMInitializeRISCVTarget();
        LLVMInitializeRISCVTargetInfo();
        LLVMInitializeRISCVTargetMC();
        LLVMInitializeRISCVAsmParser();
        LLVMInitializeRISCVAsmPrinter();
        LLVMInitializeRISCVDisassembler();
    }

    public static void initPowerPC() {
        LLVMInitializePowerPCTarget();
        LLVMInitializePowerPCTargetInfo();
        LLVMInitializePowerPCTargetMC();
        LLVMInitializePowerPCAsmParser();
        LLVMInitializePowerPCAsmPrinter();
        LLVMInitializePowerPCDisassembler();
    }

    public static void initARM() {
        LLVMInitializeARMTarget();
        LLVMInitializeARMTargetInfo();
        LLVMInitializeARMTargetMC();
        LLVMInitializeARMAsmParser();
        LLVMInitializeARMAsmPrinter();
        LLVMInitializeARMDisassembler();
    }

    public static void initAArch64() {
        LLVMInitializeAArch64Target();
        LLVMInitializeAArch64TargetInfo();
        LLVMInitializeAArch64TargetMC();
        LLVMInitializeAArch64AsmParser();
        LLVMInitializeAArch64AsmPrinter();
        LLVMInitializeAArch64Disassembler();
    }

    public static void initAVR() {
        LLVMInitializeAVRTarget();
        LLVMInitializeAVRTargetInfo();
        LLVMInitializeAVRTargetMC();
        LLVMInitializeAVRAsmParser();
        LLVMInitializeAVRAsmPrinter();
        LLVMInitializeAVRDisassembler();
    }

    public static void initX86() {
        LLVMInitializeX86Target();
        LLVMInitializeX86TargetInfo();
        LLVMInitializeX86TargetMC();
        LLVMInitializeX86AsmParser();
        LLVMInitializeX86AsmPrinter();
        LLVMInitializeX86Disassembler();
    }

    public static void initWebAssembly() {
        LLVMInitializeWebAssemblyTarget();
        LLVMInitializeWebAssemblyTargetInfo();
        LLVMInitializeWebAssemblyTargetMC();
        LLVMInitializeWebAssemblyAsmParser();
        LLVMInitializeWebAssemblyAsmPrinter();
        LLVMInitializeWebAssemblyDisassembler();
    }

    public static void loadLLVM() {
        for (final var searchPath : PATHS_TO_SEARCH) {
            for (final var directory : NAMES_TO_SEARCH) {
                final var path = Path.of(searchPath).resolve(directory);
                if (!Files.exists(path) || !Files.isDirectory(path)) {
                    continue;
                }
                final var libFolder = path.resolve("lib");
                if (!Files.exists(libFolder) || !Files.isDirectory(libFolder)) {
                    continue;
                }
                final var pathString = libFolder.toAbsolutePath().normalize().toString();
                System.setProperty("org.lwjgl.librarypath", pathString);
                System.out.printf("Loading LLVM from %s..\n\n", pathString);
                break;
            }
        }
    }

    public static void checkNatives() {
        try {
            LLVMCore.getLibrary();
        } catch (UnsatisfiedLinkError e) { // @formatter:off
            throw new IllegalStateException("""
                Please configure the LLVM (13, 14 or 15) shared libraries path with:
                \t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or
                \t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>
            """, e);
        } // @formatter:on
    }

    public static void checkStatus(final PointerBuffer buffer) throws RuntimeException {
        final var message = buffer.get(0);
        if (message != NULL) {
            final var result = buffer.getStringUTF8(0);
            nLLVMDisposeMessage(message);
            throw new RuntimeException(result);
        }
        throw new RuntimeException("Unknown error");
    }
}

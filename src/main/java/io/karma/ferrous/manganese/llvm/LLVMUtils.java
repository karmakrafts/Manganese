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
import org.lwjgl.system.MemoryStack;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static io.karma.ferrous.manganese.llvm.TargetAArch64.*;
import static io.karma.ferrous.manganese.llvm.TargetARM.*;
import static io.karma.ferrous.manganese.llvm.TargetAVR.*;
import static io.karma.ferrous.manganese.llvm.TargetMips.*;
import static io.karma.ferrous.manganese.llvm.TargetPowerPC.*;
import static io.karma.ferrous.manganese.llvm.TargetRISCV.*;
import static io.karma.ferrous.manganese.llvm.TargetWebAssembly.*;
import static org.lwjgl.llvm.LLVMCore.LLVMGetVersion;
import static org.lwjgl.llvm.LLVMCore.nLLVMDisposeMessage;
import static org.lwjgl.llvm.LLVMTargetX86.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.INTERNAL)
public final class LLVMUtils {
    private static final String[] VERSIONS = {"15", "14", "13", "12", "11"};
    private static final Pattern VERSION_PATTERN = Pattern.compile(String.format(".+(%s)", String.join("|", VERSIONS)));
    private static final String[] BASE_PATHS = {"/usr/share/lib", "/usr/local/opt", "/usr/lib", "/"};
    private static final String[] SUB_PATHS = { // @formatter:off
        "llvm-{}/build/Release",
        "llvm/build/Release",
        "llvm@{}",
        "llvm-{}",
        "llvm"
    }; // @formatter:on

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

    public static @Nullable Path getLLVMPath() {
        final var pathString = System.getProperty("org.lwjgl.librarypath");
        if (pathString != null) {
            return Path.of(pathString);
        }
        final var separator = FileSystems.getDefault().getSeparator();
        for (var searchPath : BASE_PATHS) {
            searchPath = searchPath.replaceAll("/", separator);
            for (var directory : SUB_PATHS) {
                directory = directory.replaceAll("/", separator);
                for (final var version : VERSIONS) {
                    var dirPath = directory;
                    if (directory.contains("{}")) {
                        dirPath = dirPath.replaceAll("\\{}", version);
                    }
                    final var path = Path.of(searchPath).resolve(dirPath);
                    if (!Files.exists(path) || !Files.isDirectory(path)) {
                        continue;
                    }
                    return path;
                }
            }
        }
        return null;
    }

    public static @Nullable String getLLVMVersion() {
        final var path = getLLVMPath();
        if (path == null) {
            return null;
        }
        final var libFolderPath = path.toAbsolutePath().normalize().toString();
        final var matcher = VERSION_PATTERN.matcher(libFolderPath);
        if (!matcher.find()) {
            try (final var stack = MemoryStack.stackPush()) {
                final var major = stack.callocInt(1);
                LLVMGetVersion(major, null, null);
                return Integer.toString(major.get());
            }
        }
        return matcher.group(1);
    }

    public static void loadLLVM() {
        final var path = getLLVMPath();
        if (path == null) {
            return;
        }
        final var libFolder = path.resolve("lib");
        if (!Files.exists(libFolder) || !Files.isDirectory(libFolder)) {
            return;
        }
        final var pathString = libFolder.toAbsolutePath().normalize().toString();
        System.setProperty("org.lwjgl.librarypath", pathString);
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

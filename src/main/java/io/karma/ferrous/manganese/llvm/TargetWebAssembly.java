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

import org.lwjgl.llvm.LLVMCore;

import static org.lwjgl.system.APIUtil.apiGetFunctionAddress;
import static org.lwjgl.system.JNI.invokeV;

/**
 * @author Alexander Hinze
 * @since 27/10/2023
 */
public final class TargetWebAssembly {
    TargetWebAssembly() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeWebAssemblyTargetInfo() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeWebAssemblyTarget() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeWebAssemblyTargetMC() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeWebAssemblyAsmPrinter() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeWebAssemblyAsmParser() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeWebAssemblyDisassembler() {
        long __functionAddress = TargetWebAssembly.Functions.InitializeWebAssemblyDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeWebAssemblyTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyTargetInfo"),
                InitializeWebAssemblyTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyTarget"),
                InitializeWebAssemblyTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyTargetMC"),
                InitializeWebAssemblyAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyAsmPrinter"),
                InitializeWebAssemblyAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyAsmParser"),
                InitializeWebAssemblyDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeWebAssemblyDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

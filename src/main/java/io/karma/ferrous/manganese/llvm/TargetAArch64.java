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
public final class TargetAArch64 {
    TargetAArch64() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeAArch64TargetInfo() {
        long __functionAddress = Functions.InitializeAArch64TargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAArch64Target() {
        long __functionAddress = Functions.InitializeAArch64Target;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAArch64TargetMC() {
        long __functionAddress = Functions.InitializeAArch64TargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAArch64AsmPrinter() {
        long __functionAddress = Functions.InitializeAArch64AsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAArch64AsmParser() {
        long __functionAddress = Functions.InitializeAArch64AsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAArch64Disassembler() {
        long __functionAddress = Functions.InitializeAArch64Disassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeAArch64TargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64TargetInfo"),
                InitializeAArch64Target       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64Target"),
                InitializeAArch64TargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64TargetMC"),
                InitializeAArch64AsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64AsmPrinter"),
                InitializeAArch64AsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64AsmParser"),
                InitializeAArch64Disassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAArch64Disassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

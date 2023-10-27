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
public final class LLVMTargetRISCV {
    LLVMTargetRISCV() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeRISCVTargetInfo() {
        long __functionAddress = Functions.InitializeRISCVTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeRISCVTarget() {
        long __functionAddress = Functions.InitializeRISCVTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeRISCVTargetMC() {
        long __functionAddress = Functions.InitializeRISCVTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeRISCVAsmPrinter() {
        long __functionAddress = Functions.InitializeRISCVAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeRISCVAsmParser() {
        long __functionAddress = Functions.InitializeRISCVAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeRISCVDisassembler() {
        long __functionAddress = Functions.InitializeRISCVDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeRISCVTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVTargetInfo"),
                InitializeRISCVTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVTarget"),
                InitializeRISCVTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVTargetMC"),
                InitializeRISCVAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVAsmPrinter"),
                InitializeRISCVAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVAsmParser"),
                InitializeRISCVDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeRISCVDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

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
public final class LLVMTargetPowerPC {
    LLVMTargetPowerPC() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializePowerPCTargetInfo() {
        long __functionAddress = Functions.InitializePowerPCTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializePowerPCTarget() {
        long __functionAddress = Functions.InitializePowerPCTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializePowerPCTargetMC() {
        long __functionAddress = Functions.InitializePowerPCTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializePowerPCAsmPrinter() {
        long __functionAddress = Functions.InitializePowerPCAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializePowerPCAsmParser() {
        long __functionAddress = Functions.InitializePowerPCAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializePowerPCDisassembler() {
        long __functionAddress = Functions.InitializePowerPCDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializePowerPCTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCTargetInfo"),
                InitializePowerPCTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCTarget"),
                InitializePowerPCTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCTargetMC"),
                InitializePowerPCAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCAsmPrinter"),
                InitializePowerPCAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCAsmParser"),
                InitializePowerPCDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializePowerPCDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

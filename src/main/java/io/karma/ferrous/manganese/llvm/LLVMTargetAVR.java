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
public final class LLVMTargetAVR {
    LLVMTargetAVR() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeAVRTargetInfo() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAVRTarget() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAVRTargetMC() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAVRAsmPrinter() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAVRAsmParser() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeAVRDisassembler() {
        long __functionAddress = LLVMTargetAVR.Functions.InitializeAVRDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeAVRTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRTargetInfo"),
                InitializeAVRTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRTarget"),
                InitializeAVRTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRTargetMC"),
                InitializeAVRAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRAsmPrinter"),
                InitializeAVRAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRAsmParser"),
                InitializeAVRDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeAVRDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

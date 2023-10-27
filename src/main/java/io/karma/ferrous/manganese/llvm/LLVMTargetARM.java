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
public final class LLVMTargetARM {
    LLVMTargetARM() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeARMTargetInfo() {
        long __functionAddress = Functions.InitializeARMTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeARMTarget() {
        long __functionAddress = Functions.InitializeARMTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeARMTargetMC() {
        long __functionAddress = Functions.InitializeARMTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeARMAsmPrinter() {
        long __functionAddress = Functions.InitializeARMAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeARMAsmParser() {
        long __functionAddress = Functions.InitializeARMAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeARMDisassembler() {
        long __functionAddress = Functions.InitializeARMDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeARMTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMTargetInfo"),
                InitializeARMTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMTarget"),
                InitializeARMTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMTargetMC"),
                InitializeARMAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMAsmPrinter"),
                InitializeARMAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMAsmParser"),
                InitializeARMDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeARMDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

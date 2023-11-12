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
public final class TargetMips {
    TargetMips() {
        throw new UnsupportedOperationException();
    }

    public static void LLVMInitializeMipsTargetInfo() {
        long __functionAddress = Functions.InitializeMipsTargetInfo;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeMipsTarget() {
        long __functionAddress = Functions.InitializeMipsTarget;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeMipsTargetMC() {
        long __functionAddress = Functions.InitializeMipsTargetMC;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeMipsAsmPrinter() {
        long __functionAddress = Functions.InitializeMipsAsmPrinter;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeMipsAsmParser() {
        long __functionAddress = Functions.InitializeMipsAsmParser;
        invokeV(__functionAddress);
    }

    public static void LLVMInitializeMipsDisassembler() {
        long __functionAddress = Functions.InitializeMipsDisassembler;
        invokeV(__functionAddress);
    }

    public static final class Functions {
        // @formatter:off
        public static final long
                InitializeMipsTargetInfo   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsTargetInfo"),
                InitializeMipsTarget       = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsTarget"),
                InitializeMipsTargetMC     = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsTargetMC"),
                InitializeMipsAsmPrinter   = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsAsmPrinter"),
                InitializeMipsAsmParser    = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsAsmParser"),
                InitializeMipsDisassembler = apiGetFunctionAddress(LLVMCore.getLibrary(), "LLVMInitializeMipsDisassembler");

        private Functions() {
        }
        // @formatter:on
    }
}

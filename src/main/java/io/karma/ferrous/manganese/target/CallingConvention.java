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

package io.karma.ferrous.manganese.target;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public enum CallingConvention {
    // @formatter:off
    CDECL       ("cdecl",       LLVMCore.LLVMCCallConv),
    ANYREG      ("anyreg",      LLVMCore.LLVMAnyRegCallConv),
    REGCALL     ("regcall",     LLVMCore.LLVMX86RegCallCallConv),
    STDCALL     ("stdcall",     LLVMCore.LLVMX86StdcallCallConv),
    THISCALL    ("thiscall",    LLVMCore.LLVMX86ThisCallCallConv),
    FASTCALL    ("fastcall",    LLVMCore.LLVMX86FastcallCallConv),
    VECTORCALL  ("vectorcall",  LLVMCore.LLVMX86VectorCallCallConv),
    SWIFTCALL   ("swiftcall",   LLVMCore.LLVMSwiftCallConv),
    MS          ("ms",          LLVMCore.LLVMWin64CallConv),
    SYSV        ("sysv",        LLVMCore.LLVMX8664SysVCallConv);
    // @formatter:on

    // @formatter:off
    public static final List<String> EXPECTED_VALUES = Arrays.stream(values())
        .map(CallingConvention::getText)
        .collect(Collectors.toList());
    // @formatter:on
    private final String text;
    private final int llvmType;

    CallingConvention(final String text, final int llvmType) {
        this.text = text;
        this.llvmType = llvmType;
    }

    public String getText() {
        return text;
    }

    public int getLlvmType() {
        return llvmType;
    }
}

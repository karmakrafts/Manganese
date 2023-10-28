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

package io.karma.ferrous.manganese.util;

import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.llvm.LLVMCore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public enum CallingConvention {
    // @formatter:off
    CDECL       ("cdecl",       m -> LLVMCore.LLVMCCallConv),
    ANYREG      ("anyreg",      m -> LLVMCore.LLVMAnyRegCallConv),
    REGCALL     ("regcall",     m -> LLVMCore.LLVMX86RegCallCallConv),
    STDCALL     ("stdcall",     m -> LLVMCore.LLVMX86StdcallCallConv),
    THISCALL    ("thiscall",    m -> LLVMCore.LLVMX86ThisCallCallConv),
    FASTCALL    ("fastcall",    m -> LLVMCore.LLVMX86FastcallCallConv),
    VECTORCALL  ("vectorcall",  m -> LLVMCore.LLVMX86VectorCallCallConv),
    SWIFTCALL   ("swiftcall",   m -> LLVMCore.LLVMSwiftCallConv),
    WIN64       ("win64",       m -> LLVMCore.LLVMWin64CallConv),
    SYSV        ("sysv",        m -> LLVMCore.LLVMX8664SysVCallConv),
    INTERRUPT   ("interrupt",   CallingConvention::getInterruptCallConv),
    SIGNAL      ("signal",      CallingConvention::getSignalCallConv);
    // @formatter:on

    // @formatter:off
    public static final List<String> EXPECTED_VALUES = Arrays.stream(values())
        .map(CallingConvention::getText)
        .collect(Collectors.toList());
    // @formatter:on
    private final String text;
    private final ToIntFunction<TargetMachine> valueProvider;

    CallingConvention(final String text, final ToIntFunction<TargetMachine> valueProvider) {
        this.text = text;
        this.valueProvider = valueProvider;
    }

    public static Optional<CallingConvention> findByText(final String text) {
        return Arrays.stream(values()).filter(conv -> conv.text.equals(text)).findFirst();
    }

    private static int getInterruptCallConv(final TargetMachine machine) {
        return switch (machine.getTarget().getArchitecture()) { // @formatter:off
            case X86, X86_64    -> LLVMCore.LLVMX86INTRCallConv;
            case ARM, AARCH64   -> LLVMCore.LLVMAVRINTRCallConv;
            default             -> LLVMCore.LLVMCCallConv;
        }; // @formatter:on
    }

    private static int getSignalCallConv(final TargetMachine machine) {
        return switch(machine.getTarget().getArchitecture()) { // @formatter:off
            case ARM, AARCH64   -> LLVMCore.LLVMAVRSIGNALCallConv;
            default             -> LLVMCore.LLVMCCallConv;
        }; // @formatter:on
    }

    public String getText() {
        return text;
    }

    public int getLLVMValue(final TargetMachine machine) {
        return valueProvider.applyAsInt(machine);
    }
}

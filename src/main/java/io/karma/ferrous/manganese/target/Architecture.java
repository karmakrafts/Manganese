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

import io.karma.kommons.util.SystemInfo;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public enum Architecture {
    // @formatter:off
    ARM         ("arm",         "arm9e"),
    THUMB       ("thumb",       "arm9e"),
    THUMB_EB    ("thumbeb",     "arm9e"),
    AARCH64     ("aarch64",     "cortex-a72"),
    AARCH64_32  ("aarch64_32",  "cortex-a72"),
    AARCH64_BE  ("aarch64_be",  "cortex-a72"),
    X86         ("i386",        "i386"),
    X86_64      ("x86_64",      "x86-64-v3"),
    RISCV32     ("riscv32",     "sifive-7-rv32"),
    RISCV64     ("riscv64",     "sifive-7-rv64"),
    WASM32      ("wasm32",      "generic"),
    WASM64      ("wasm64",      "generic"),
    MIPS        ("mips",        "mips32r6"),
    MIPS_EL     ("mipsel",      "mips32r6"),
    MIPS64      ("mips64",      "mips32r6"),
    MIPS64_EL   ("mips64el",    "mips32r6"),
    PPC32       ("ppc32",       "ppc32"),
    PPC32_LE    ("ppc32le",     "ppc32"),
    PPC64       ("ppc64",       "ppc64"),
    PPC64_LE    ("ppc64le",     "ppc64le"),
    AVR         ("avr",         "atmega48"),
    AMDGCN      ("amdgcn",      "generic"),
    R600        ("r600",        "r600");
    // @formatter:on

    private final String name;
    private final String defaultCPU;

    Architecture(final String name, final String defaultCPU) {
        this.name = name;
        this.defaultCPU = defaultCPU;
    }

    public static Architecture getHostArchitecture() {
        if (SystemInfo.isIA32()) {
            return Architecture.X86;
        }
        if (SystemInfo.isAMD64()) {
            return Architecture.X86_64;
        }
        if (SystemInfo.isARM()) {
            return Architecture.ARM;
        }
        if (SystemInfo.isARM64()) {
            return Architecture.AARCH64;
        }
        if (SystemInfo.isRiscV64()) {
            return Architecture.RISCV64;
        }
        if (SystemInfo.isRiscV32()) {
            return Architecture.RISCV32;
        }
        throw new UnsupportedOperationException("Unknown host architecture");
    }

    public static Optional<Architecture> byName(final String name) {
        return Arrays.stream(values()).filter(arch -> arch.name.equals(name)).findFirst();
    }

    public String getName() {
        return name;
    }

    public String getDefaultCPU() {
        return defaultCPU;
    }
}

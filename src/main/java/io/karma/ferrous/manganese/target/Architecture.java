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

import java.util.ArrayList;
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
    THUMB_EB    ("thumbeb",     "arm9e",         "thumbbe"),
    AARCH64     ("aarch64",     "cortex-a72",    "arm64"),
    AARCH64_32  ("aarch64_32",  "cortex-a72",    "aarch64-32"),
    AARCH64_BE  ("aarch64_be",  "cortex-a72",    "aarch64-be", "aarch64_eb"),
    X86         ("i386",        "i386",          "i486", "i586", "i686", "x86", "x32"),
    X86_64      ("x86_64",      "x86-64-v3",     "amd64", "x86-64", "x64"),
    RISCV32     ("riscv32",     "sifive-7-rv32", "risc-v32", "riscv-32", "risc_v32", "riscv_32"),
    RISCV64     ("riscv64",     "sifive-7-rv64", "risc-v64", "riscv-64", "risc_v64", "riscv_64"),
    WASM32      ("wasm32",      "generic",       "wasm-32", "wasm_32"),
    WASM64      ("wasm64",      "generic",       "wasm-64", "wasm_64"),
    MIPS        ("mips",        "mips32r6",      "mips32", "mips-32", "mips_32"),
    MIPS_EL     ("mipsel",      "mips32r6",      "mips32el", "mips-32el", "mips_32el"),
    MIPS64      ("mips64",      "mips64r6",      "mips-64", "mips_64"),
    MIPS64_EL   ("mips64el",    "mips64r6",      "mips-64el", "mips_64el"),
    PPC32       ("ppc32",       "ppc32",         "ppc", "powerpc", "powerpc32"),
    PPC32_LE    ("ppc32le",     "ppc32",         "ppcle", "powerpcle", "powerpc32le", "powerpc-32le", "powerpc_32le"),
    PPC64       ("ppc64",       "ppc64",         "powerpc64", "powerpc-64", "powerpc_64"),
    PPC64_LE    ("ppc64le",     "ppc64le",       "powerpc64le", "powerpc-64le", "powerpc_64le"),
    AVR         ("avr",         "atmega48",      "atmega");
    // @formatter:on

    private final String name;
    private final String defaultCPU;
    private final String[] aliases;

    Architecture(final String name, final String defaultCPU, final String... aliases) {
        this.name = name;
        this.defaultCPU = defaultCPU;

        final var numAliases = aliases.length;
        this.aliases = new String[numAliases + 1];
        System.arraycopy(aliases, 0, this.aliases, 0, numAliases);
        this.aliases[numAliases] = name;
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

    public String makePattern() {
        final var builder = new StringBuilder();
        final var names = new ArrayList<>(Arrays.asList(aliases));
        names.add(name);
        final var numNames = names.size();
        for (var i = 0; i < numNames; i++) {
            builder.append(names.get(i));
            if (i < numNames - 1) {
                builder.append('|');
            }
        }
        return builder.toString();
    }

    public String getName() {
        return name;
    }

    public String getDefaultCPU() {
        return defaultCPU;
    }

    public String[] getAliases() {
        return aliases;
    }

    @Override
    public String toString() {
        return name;
    }
}

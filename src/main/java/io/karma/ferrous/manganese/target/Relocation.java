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
import org.lwjgl.llvm.LLVMTargetMachine;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.STABLE)
public enum Relocation {
    // @formatter:off
    DEFAULT     ("default",         LLVMTargetMachine.LLVMRelocDefault,      false),
    STATIC      ("static",          LLVMTargetMachine.LLVMRelocStatic,       false),
    PIC         ("pic",             LLVMTargetMachine.LLVMRelocPIC,          true ),
    DYN_NO_PIC  ("dynamic_nopic",   LLVMTargetMachine.LLVMRelocDynamicNoPic, true ),
    ROPI        ("ropi",            LLVMTargetMachine.LLVMRelocROPI,         true ),
    RWPI        ("rwpi",            LLVMTargetMachine.LLVMRelocRWPI,         true ),
    ROPI_RWPI   ("ropi_rwpi",       LLVMTargetMachine.LLVMRelocROPI_RWPI,    true );
    // @formatter:on

    private final String name;
    private final int llvmValue;
    private final boolean isDynamic;

    Relocation(final String name, final int llvmValue, final boolean isDynamic) {
        this.name = name;
        this.llvmValue = llvmValue;
        this.isDynamic = isDynamic;
    }

    public static Optional<Relocation> byName(final String name) {
        return Arrays.stream(values()).filter(reloc -> reloc.name.equals(name)).findFirst();
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public String getName() {
        return name;
    }

    public int getLLVMValue() {
        return llvmValue;
    }

    @Override
    public String toString() {
        return name;
    }
}

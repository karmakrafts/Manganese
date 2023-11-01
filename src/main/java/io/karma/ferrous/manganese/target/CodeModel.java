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
public enum CodeModel {
    // @formatter:off
    DEFAULT ("default", LLVMTargetMachine.LLVMCodeModelDefault),
    KERNEL  ("kernel",  LLVMTargetMachine.LLVMCodeModelKernel),
    LARGE   ("large",   LLVMTargetMachine.LLVMCodeModelLarge),
    MEDIUM  ("medium",  LLVMTargetMachine.LLVMCodeModelMedium),
    SMALL   ("small",   LLVMTargetMachine.LLVMCodeModelSmall),
    TINY    ("tiny",    LLVMTargetMachine.LLVMCodeModelTiny);
    // @formatter:on

    private final String name;
    private final int llvmValue;

    CodeModel(final String name, final int llvmValue) {
        this.name = name;
        this.llvmValue = llvmValue;
    }

    public static Optional<CodeModel> byName(final String name) {
        return Arrays.stream(values()).filter(model -> model.name.equals(name)).findFirst();
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

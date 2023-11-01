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

import org.lwjgl.llvm.LLVMTargetMachine;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
public enum FileType {
    // @formatter:off
    ASSEMBLY("S", LLVMTargetMachine.LLVMAssemblyFile),
    OBJECT  ("o", LLVMTargetMachine.LLVMObjectFile);
    // @formatter:on

    private final String extension;
    private final int llvmValue;

    FileType(final String extension, final int llvmValue) {
        this.extension = extension;
        this.llvmValue = llvmValue;
    }

    public static Optional<FileType> byExtension(final String extension) {
        return Arrays.stream(values()).filter(type -> type.extension.equals(extension)).findFirst();
    }

    public String getExtension() {
        return extension;
    }

    public int getLLVMValue() {
        return llvmValue;
    }

    @Override
    public String toString() {
        return extension;
    }
}

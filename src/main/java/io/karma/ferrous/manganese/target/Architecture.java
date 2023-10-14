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
import org.lwjgl.llvm.LLVMTargetX86;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public enum Architecture {
    // @formatter:off
    UNKNOWN ("unknown", SystemInfo.is64Bit() ? 8 : 4, () -> {}),
    ARM     ("arm",     4, () -> {}),
    AARCH64 ("aarch64", 8, () -> {}),
    X86     ("x86",     4, LLVMTargetX86::LLVMInitializeX86Target),
    X86_64  ("x86_64",  8, LLVMTargetX86::LLVMInitializeX86Target),
    RISCV_32("riscv32", 4, () -> {}),
    RISCV_64("riscv64", 8, () -> {}),
    WASM_32 ("wasm32",  4, () -> {}),
    WASM_64 ("wasm64",  8, () -> {});
    // @formatter:on

    private final String name;
    private final int pointerSize;
    private final Runnable initClosure;

    Architecture(final String name, final int pointerSize, final Runnable initClosure) {
        this.name = name;
        this.pointerSize = pointerSize;
        this.initClosure = initClosure;
    }

    public String getName() {
        return name;
    }

    public int getPointerSize() {
        return pointerSize;
    }

    public void init() {
        initClosure.run();
    }
}

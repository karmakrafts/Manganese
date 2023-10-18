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

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.lwjgl.PointerBuffer;
import org.lwjgl.llvm.LLVMCore;

import static org.lwjgl.llvm.LLVMCore.nLLVMDisposeMessage;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.INTERNAL)
public final class LLVMUtils {
    // @formatter:off
    private LLVMUtils() {}
    // @formatter:on

    public static void checkNatives() {
        try {
            LLVMCore.getLibrary();
        }
        catch (UnsatisfiedLinkError e) { // @formatter:off
            throw new IllegalStateException("""
                Please configure the LLVM (13, 14 or 15) shared libraries path with:
                \t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or
                \t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>
            """, e);
        } // @formatter:on
    }

    public static void checkStatus(final PointerBuffer buffer) throws RuntimeException {
        final var message = buffer.get(0);
        if (message != NULL) {
            final var result = buffer.getStringUTF8(0);
            nLLVMDisposeMessage(message);
            throw new RuntimeException(result);
        }
        throw new RuntimeException("Unknown error");
    }
}

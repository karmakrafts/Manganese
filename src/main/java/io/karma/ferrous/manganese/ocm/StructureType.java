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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.target.Target;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public record StructureType(String name, boolean isPacked, Type... fieldTypes) {
    public StructureType(final String name, final Type... fieldTypes) {
        this(name, false, fieldTypes);
    }

    public long materialize(final Target target) {
        try (final var stack = MemoryStack.stackPush()) {
            final var numFields = fieldTypes.length;
            final var fields = stack.callocPointer(numFields);
            for (var i = 0; i < numFields; i++) {
                fields.put(i, fieldTypes[i].materialize(target));
            }
            return LLVMCore.LLVMStructType(fields, isPacked);
        }
    }

    public boolean isPacked() {
        return isPacked;
    }

    public Type[] getFieldTypes() {
        return fieldTypes;
    }

    public String getName() {
        return name;
    }
}

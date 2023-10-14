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

package io.karma.ferrous.manganese.type;

import io.karma.ferrous.manganese.target.Target;
import org.lwjgl.llvm.LLVMCore;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
public final class Structure {
    private final boolean isPacked;
    private final List<Type> fieldTypes;

    public Structure(final boolean isPacked, final Type... fieldTypes) {
        this.isPacked = isPacked;
        this.fieldTypes = Arrays.asList(fieldTypes);
    }

    public Structure(final Type... fieldTypes) {
        this(false, fieldTypes);
    }

    public long materializeType(final Target target) {
        try (final var stack = MemoryStack.stackPush()) {
            final var numFields = fieldTypes.size();
            final var fields = stack.callocPointer(numFields);
            for (var i = 0; i < numFields; i++) {
                fields.put(i, fieldTypes.get(i).materialize(target));
            }
            return LLVMCore.LLVMStructType(fields, isPacked);
        }
    }

    public boolean isPacked() {
        return isPacked;
    }

    public List<Type> getFieldTypes() {
        return fieldTypes;
    }
}

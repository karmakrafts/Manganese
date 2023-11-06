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

import io.karma.ferrous.manganese.compiler.CompileContext;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public interface BlockContext {
    String DEFAULT_BLOCK = "_entry";

    CompileContext getCompileContext();

    Function getFunction();

    @Nullable BlockBuilder getCurrent();

    @Nullable BlockBuilder getLast();

    BlockBuilder getOrCreate(final String name);

    void pushCurrent(final BlockBuilder builder);

    BlockBuilder popCurrent();

    default BlockBuilder getCurrentOrCreate(final String name) {
        var current = getCurrent();
        if (current != null) {
            return current;
        }
        return getOrCreate(name);
    }

    default BlockBuilder getCurrentOrCreate() {
        var current = getCurrent();
        if (current != null) {
            return current;
        }
        return getOrCreate(DEFAULT_BLOCK);
    }
}

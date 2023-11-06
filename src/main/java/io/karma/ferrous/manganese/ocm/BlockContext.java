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

import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public interface BlockContext {
    String DEFAULT_BLOCK = "entry";

    Function getFunction();

    @Nullable BlockBuilder getCurrentBlock();

    @Nullable BlockBuilder getLastBlock();

    BlockBuilder createBlock(final String name);

    @Nullable BlockBuilder getBlockBuilder(final String name);

    default BlockBuilder getCurrentOrCreate(final String name) {
        var current = getCurrentBlock();
        if (current != null) {
            return current;
        }
        return createBlock(name);
    }

    default BlockBuilder getCurrentOrCreate() {
        var current = getCurrentBlock();
        if (current != null) {
            return current;
        }
        return createBlock(DEFAULT_BLOCK);
    }
}

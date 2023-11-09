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

package io.karma.ferrous.manganese.ocm.ir;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 06/11/2023
 */
@API(status = API.Status.INTERNAL)
public interface IRContext {
    String DEFAULT_BLOCK = "";

    long getParameter(final Identifier name);

    Module getModule();

    CompileContext getCompileContext();

    Function getFunction();

    @Nullable IRBuilder getCurrent();

    @Nullable IRBuilder getLast();

    IRBuilder getOrCreate(final String name);

    void pushCurrent(final IRBuilder builder);

    IRBuilder popCurrent();

    default IRBuilder getCurrentOrCreate(final String name) {
        var current = getCurrent();
        if (current != null) {
            return current;
        }
        return getOrCreate(name);
    }

    default IRBuilder getCurrentOrCreate() {
        var current = getCurrent();
        if (current != null) {
            return current;
        }
        return getOrCreate(DEFAULT_BLOCK);
    }
}

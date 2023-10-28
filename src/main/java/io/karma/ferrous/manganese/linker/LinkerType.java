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

package io.karma.ferrous.manganese.linker;

import org.apiguardian.api.API;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author Alexander Hinze
 * @since 28/10/2023
 */
@API(status = API.Status.STABLE)
public enum LinkerType {
    // @formatter:off
    ELF  ("elf",    "ld.lld",   ELFLinker::new),
    MACHO("macho",  "ld64.lld", MachOLinker::new),
    COFF ("coff",   "lld-link", COFFLinker::new),
    WASM ("wasm",   "wasm-ld",  WASMLinker::new);
    // @formatter:on

    private final String name;
    private final String alias;
    private final Supplier<Linker> factory;

    LinkerType(final String name, final String alias, final Supplier<Linker> factory) {
        this.name = name;
        this.alias = alias;
        this.factory = factory;
    }

    public static Optional<LinkerType> byName(final String name) {
        return Arrays.stream(values()).filter(type -> type.name.equals(name)).findFirst();
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public Linker create() {
        return factory.get();
    }
}

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

import io.karma.ferrous.manganese.linker.LinkerType;
import io.karma.kommons.util.SystemInfo;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.STABLE)
public enum Platform {
    // @formatter:off
    UNKNOWN ("unknown", LinkerType.ELF),
    LINUX   ("linux",   LinkerType.ELF),
    WINDOWS ("windows", LinkerType.COFF),
    MACOS   ("macos",   LinkerType.MACHO);
    // @formatter:on

    private final String name;
    private final LinkerType defaultLinkerType;

    Platform(final String name, final LinkerType defaultLinkerType) {
        this.name = name;
        this.defaultLinkerType = defaultLinkerType;
    }

    public static Platform getHostPlatform() {
        final var current = SystemInfo.Platform.getCurrent();
        if (current == SystemInfo.Platform.UNIX) {
            return LINUX;
        }
        if (current == SystemInfo.Platform.WINDOWS) {
            return WINDOWS;
        }
        if (current == SystemInfo.Platform.MAC) {
            return MACOS;
        }
        return UNKNOWN;
    }

    public static Optional<Platform> byName(final String name) {
        return Arrays.stream(values()).filter(platform -> platform.name.equals(name)).findFirst();
    }

    public String getName() {
        return name;
    }

    public LinkerType getDefaultLinkerType() {
        return defaultLinkerType;
    }

    @Override
    public String toString() {
        return name;
    }
}

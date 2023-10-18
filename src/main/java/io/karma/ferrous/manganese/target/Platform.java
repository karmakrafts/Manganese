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
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 18/10/2023
 */
@API(status = Status.STABLE)
public enum Platform {
    // @formatter:off
    UNKNOWN ("unknown"),
    LINUX   ("linux"),
    WINDOWS ("windows"),
    MACOS   ("macos");
    // @formatter:on

    private final String name;

    Platform(final String name) {
        this.name = name;
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

    public String getName() {
        return name;
    }
}

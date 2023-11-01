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

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public enum ABI {
    // @formatter:off
    GNU         ("gnu"),
    GNU_EABI    ("gnueabi"),
    MSVC        ("msvc"),
    ANDROID     ("android"),
    ANDROID_EABI("androideabi");
    // @formatter:on

    private final String name;

    ABI(final String name) {
        this.name = name;
    }

    public static ABI getHostABI() {
        switch(SystemInfo.Platform.getCurrent()) { // @formatter:off
            case WINDOWS:   return MSVC;
            default:        return GNU;
        } // @formatter:on
    }

    public static Optional<ABI> byName(final String name) {
        return Arrays.stream(values()).filter(abi -> abi.name.equals(name)).findFirst();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

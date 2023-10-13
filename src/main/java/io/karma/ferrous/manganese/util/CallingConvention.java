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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.STABLE)
public enum CallingConvention {
    // @formatter:off
    CDECL       ("cdecl"),
    STDCALL     ("stdcall"),
    THISCALL    ("thiscall"),
    FASTCALL    ("fastcall"),
    VECTORCALL  ("vectorcall"),
    MS          ("ms"),
    SYSV        ("sysv");
    // @formatter:on

    // @formatter:off
    public static final List<String> EXPECTED_VALUES = Arrays.stream(values())
        .map(CallingConvention::getText)
        .collect(Collectors.toList());
    // @formatter:on
    private final String text;

    CallingConvention(final String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

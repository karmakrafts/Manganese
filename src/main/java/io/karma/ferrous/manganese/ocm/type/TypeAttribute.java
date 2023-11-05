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

package io.karma.ferrous.manganese.ocm.type;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import java.util.function.Function;

/**
 * @author Alexander Hinze
 * @since 14/10/2023
 */
@API(status = Status.INTERNAL)
public enum TypeAttribute {
    // @formatter:off
    SLICE    (type -> String.format("[%s]", type), 1, 1),
    POINTER  (type -> String.format("*%s", type),  1, 0),
    REFERENCE(type -> String.format("&%s", type),  1, 0);
    // @formatter:on

    private final Function<String, String> formatter;
    private final int lhw;
    private final int rhw;

    TypeAttribute(final Function<String, String> formatter, final int lhw, final int rhw) {
        this.formatter = formatter;
        this.lhw = lhw;
        this.rhw = rhw;
    }

    public String format(final String type) {
        return formatter.apply(type);
    }

    public int getLHWidth() {
        return lhw;
    }

    public int getRHWidth() {
        return rhw;
    }
}

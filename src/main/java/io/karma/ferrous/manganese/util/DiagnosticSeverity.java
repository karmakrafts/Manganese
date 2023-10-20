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

import java.util.Arrays;
import java.util.Optional;

/**
 * @author Alexander Hinze
 * @since 20/10/2023
 */
public enum DiagnosticSeverity {
    // @formatter:off
    ERROR   (0),
    WARNING (1),
    REMARK  (2),
    NOTE    (3);
    // @formatter:on

    private final int llvmValue; // Use separate value from ordinal to guarantee ABI compat

    DiagnosticSeverity(final int llvmValue) {
        this.llvmValue = llvmValue;
    }

    public static Optional<DiagnosticSeverity> byValue(final int value) {
        return Arrays.stream(values()).filter(sev -> sev.llvmValue == value).findFirst();
    }

    public int getLlvmValue() {
        return llvmValue;
    }
}

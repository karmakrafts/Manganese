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

package io.karma.ferrous.manganese.ocm.scope;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 15/10/2023
 */
@API(status = Status.INTERNAL)
public enum ScopeType {
    // @formatter:off
    GLOBAL      (true),
    FILE        (true),
    MODULE_FILE (true),
    MODULE      (true),
    STRUCT      (false),
    CLASS       (false),
    ENUM_CLASS  (false),
    ENUM        (false),
    TRAIT       (false),
    ATTRIBUTE   (false),
    INTERFACE   (false),
    CONSTRUCTOR (false),
    DESTRUCTOR  (false),
    FUNCTION    (false),
    IF          (false),
    ELSE_IF     (false),
    ELSE        (false),
    WHEN        (false),
    FOR         (false),
    WHILE       (false),
    DO          (false),
    LOOP        (false),
    LABEL_BLOCK (false);
    // @formatter:on

    private final boolean isGlobal;

    ScopeType(final boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public boolean isGlobal() {
        return isGlobal;
    }
}

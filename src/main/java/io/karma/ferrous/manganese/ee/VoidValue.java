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

package io.karma.ferrous.manganese.ee;

import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import org.apiguardian.api.API;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 12/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class VoidValue implements GenericValue {
    static final VoidValue INSTANCE = new VoidValue();

    // @formatter:off
    private VoidValue() {}
    // @formatter:on

    @Override
    public Type getType() {
        return BuiltinType.VOID;
    }

    @Override
    public long getAddress() {
        return NULL;
    }

    @Override
    public void dispose() {

    }
}

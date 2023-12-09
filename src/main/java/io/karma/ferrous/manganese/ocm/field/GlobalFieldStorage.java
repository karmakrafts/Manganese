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

package io.karma.ferrous.manganese.ocm.field;

import io.karma.ferrous.manganese.ocm.ValueStorage;
import io.karma.ferrous.manganese.ocm.expr.Expression;
import io.karma.ferrous.manganese.ocm.ir.IRContext;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Hinze
 * @since 16/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class GlobalFieldStorage implements ValueStorage {
    @Override
    public Identifier getName() {
        return null;
    }

    @Override
    public @Nullable Expression getValue() {
        return null;
    }

    @Override
    public void setValue(final @Nullable Expression value) {

    }

    @Override
    public void setInitialized() {

    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public boolean isMutated() {
        return false;
    }

    @Override
    public void notifyMutation() {

    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public long getAddress(final TargetMachine targetMachine, final IRContext irContext) {
        return 0;
    }
}

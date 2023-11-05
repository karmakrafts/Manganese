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

package io.karma.ferrous.manganese.ocm.constant;

import io.karma.ferrous.manganese.ocm.BlockBuilder;
import io.karma.ferrous.manganese.ocm.type.BuiltinType;
import io.karma.ferrous.manganese.ocm.type.Type;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

/**
 * @author Alexander Hinze
 * @since 22/10/2023
 */
@API(status = Status.INTERNAL)
public final class VoidConstant implements Constant {
    public static final VoidConstant INSTANCE = new VoidConstant();

    // @formatter:off
    private VoidConstant() {}
    // @formatter:on

    @Override
    public Type getType() {
        return BuiltinType.VOID;
    }

    @Override
    public long materialize(final TargetMachine targetMachine, final BlockBuilder builder) {
        throw new UnsupportedOperationException("Cannot materialize void constant");
    }

    @Override
    public void emit(final TargetMachine targetMachine, final BlockBuilder builder) {
    }
}

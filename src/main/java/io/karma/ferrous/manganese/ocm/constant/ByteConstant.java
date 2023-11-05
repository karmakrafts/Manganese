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
import org.lwjgl.llvm.LLVMCore;

/**
 * @author Alexander Hinze
 * @since 16/10/2023
 */
@API(status = Status.INTERNAL)
public record ByteConstant(byte value, boolean isUnsigned) implements Constant {
    @Override
    public Type getType() {
        return isUnsigned ? BuiltinType.U8 : BuiltinType.I8;
    }

    @Override
    public long materialize(final TargetMachine targetMachine, final BlockBuilder builder) {
        return LLVMCore.LLVMConstInt(getType().materialize(targetMachine), value, false);
    }

    @Override
    public void emit(final TargetMachine targetMachine, final BlockBuilder builder) {

    }
}

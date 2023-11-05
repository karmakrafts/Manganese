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

package io.karma.ferrous.manganese.ocm;

import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;

import static org.lwjgl.llvm.LLVMCore.LLVMAppendBasicBlockInContext;
import static org.lwjgl.llvm.LLVMCore.LLVMGetGlobalContext;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public record FunctionBody(Function function, Statement... statements) {
    public long materialize(final Module module, final TargetMachine targetMachine) {
        final var fnAddress = function.materialize(module, targetMachine);
        final var name = function.getQualifiedName().toInternalName();
        final var blockAddress = LLVMAppendBasicBlockInContext(LLVMGetGlobalContext(), fnAddress, name);
        final var builder = BlockBuilder.getInstance(blockAddress);
        for (final var statement : statements) {
            statement.emit(targetMachine, builder);
        }
        return blockAddress;
    }
}

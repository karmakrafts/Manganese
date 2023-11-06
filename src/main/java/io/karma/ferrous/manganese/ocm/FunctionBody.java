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
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

import static org.lwjgl.llvm.LLVMCore.LLVMAppendBasicBlockInContext;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public record FunctionBody(Statement... statements) {
    public void append(final Function function, final Module module, final TargetMachine targetMachine) {
        final var context = new BlockContextImpl(module, targetMachine, function);
        for (final var statement : statements) {
            statement.emit(targetMachine, context);
        }
        context.dispose();
    }

    private static final class BlockContextImpl implements BlockContext {
        private final Module module;
        private final TargetMachine targetMachine;
        private final Function function;
        private final LinkedHashMap<String, BlockBuilder> builders = new LinkedHashMap<>();
        private BlockBuilder current;
        private BlockBuilder last;

        public BlockContextImpl(final Module module, final TargetMachine targetMachine, final Function function) {
            this.module = module;
            this.targetMachine = targetMachine;
            this.function = function;
        }

        public void dispose() {
            builders.values().forEach(BlockBuilder::dispose);
        }

        @Override
        public Function getFunction() {
            return function;
        }

        @Override
        public @Nullable BlockBuilder getCurrentBlock() {
            return current;
        }

        @Override
        public @Nullable BlockBuilder getLastBlock() {
            return last;
        }

        @Override
        public BlockBuilder createBlock(final String name) {
            last = current;
            return current = builders.computeIfAbsent(name, n -> {
                final var fnAddress = function.materializePrototype(module, targetMachine);
                final var context = module.getContext();
                final var blockAddress = LLVMAppendBasicBlockInContext(context, fnAddress, name);
                return new BlockBuilder(module, targetMachine, blockAddress, context);
            });
        }

        @Override
        public @Nullable BlockBuilder getBlockBuilder(final String name) {
            return builders.get(name);
        }
    }
}

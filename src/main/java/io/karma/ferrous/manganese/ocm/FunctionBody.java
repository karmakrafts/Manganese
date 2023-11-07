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

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.statement.Statement;
import io.karma.ferrous.manganese.target.TargetMachine;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

import static org.lwjgl.llvm.LLVMCore.LLVMAppendBasicBlockInContext;

/**
 * @author Alexander Hinze
 * @since 05/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class FunctionBody {
    private final ArrayList<Statement> statements;
    private boolean isAppended;

    public FunctionBody(final Statement... statements) {
        this.statements = new ArrayList<>(Arrays.asList(statements));
    }

    public ArrayList<Statement> getStatements() {
        return statements;
    }

    public void append(final CompileContext compileContext, final Function function, final Module module,
                       final TargetMachine targetMachine) {
        if (isAppended) {
            return;
        }
        final var context = new BlockContextImpl(compileContext, module, targetMachine, function);
        for (final var statement : statements) {
            statement.emit(targetMachine, context);
        }
        context.dispose();
        isAppended = true;
    }

    private static final class BlockContextImpl implements BlockContext {
        private final CompileContext compileContext;
        private final Module module;
        private final TargetMachine targetMachine;
        private final Function function;
        private final HashMap<String, BlockBuilder> builders = new HashMap<>();
        private final Stack<BlockBuilder> stack = new Stack<>();

        public BlockContextImpl(final CompileContext compileContext, final Module module,
                                final TargetMachine targetMachine, final Function function) {
            this.compileContext = compileContext;
            this.module = module;
            this.targetMachine = targetMachine;
            this.function = function;
        }

        public void dispose() {
            builders.values().forEach(BlockBuilder::dispose);
        }

        @Override
        public void pushCurrent(final BlockBuilder builder) {
            stack.push(builder);
        }

        @Override
        public BlockBuilder popCurrent() {
            return stack.pop();
        }

        @Override
        public CompileContext getCompileContext() {
            return compileContext;
        }

        @Override
        public Function getFunction() {
            return function;
        }

        @Override
        public @Nullable BlockBuilder getCurrent() {
            if (stack.isEmpty()) {
                return null;
            }
            return stack.peek();
        }

        @Override
        public @Nullable BlockBuilder getLast() {
            if (stack.size() < 2) {
                return null;
            }
            return stack.get(1);
        }

        @Override
        public BlockBuilder getOrCreate(final String name) {
            final var builder = builders.computeIfAbsent(name, n -> {
                final var fnAddress = function.materializePrototype(module, targetMachine);
                final var context = module.getContext();
                final var blockAddress = LLVMAppendBasicBlockInContext(context, fnAddress, name);
                return new BlockBuilder(this, module, targetMachine, blockAddress, context);
            });
            stack.push(builder);
            return builder;
        }
    }
}

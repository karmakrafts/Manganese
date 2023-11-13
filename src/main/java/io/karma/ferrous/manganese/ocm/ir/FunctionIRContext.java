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

package io.karma.ferrous.manganese.ocm.ir;

import io.karma.ferrous.manganese.compiler.CompileContext;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.ocm.function.Function;
import io.karma.ferrous.manganese.target.TargetMachine;
import io.karma.ferrous.manganese.util.Identifier;
import org.apiguardian.api.API;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.llvm.LLVMCore;

import java.util.HashMap;

import static org.lwjgl.llvm.LLVMCore.LLVMAppendBasicBlockInContext;
import static org.lwjgl.llvm.LLVMCore.LLVMDeleteBasicBlock;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 10/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class DefaultIRContext implements IRContext {
    private final CompileContext compileContext;
    private final Module module;
    private final TargetMachine targetMachine;
    private final Function function;
    private final HashMap<String, IRBuilder> builders = new HashMap<>();
    private IRBuilder currentBuilder;

    public DefaultIRContext(final CompileContext compileContext, final Module module, final TargetMachine targetMachine,
                            final Function function) {
        this.compileContext = compileContext;
        this.module = module;
        this.targetMachine = targetMachine;
        this.function = function;
    }

    public void dispose() {
        builders.values().forEach(IRBuilder::dispose);
    }

    @Override
    public long getParameter(final Identifier name) {
        final var params = function.getParameters();
        final var numParams = params.length;
        final var address = function.materializePrototype(module, targetMachine);
        for (var i = 0; i < numParams; i++) {
            if (!params[i].getName().equals(name)) {
                continue;
            }
            return LLVMCore.LLVMGetParam(address, i);
        }
        return NULL;
    }

    @Override
    public CompileContext getCompileContext() {
        return compileContext;
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public Function getFunction() {
        return function;
    }

    @Override
    public @Nullable IRBuilder getCurrent() {
        return currentBuilder;
    }

    @Override
    public IRBuilder getOrCreate(final String name) {
        return currentBuilder = builders.computeIfAbsent(name, n -> {
            final var fnAddress = function.materializePrototype(module, targetMachine);
            final var context = module.getContext();
            final var blockAddress = LLVMAppendBasicBlockInContext(context, fnAddress, name);
            return new IRBuilder(this, module, targetMachine, blockAddress, context);
        });
    }

    @Override
    public void drop(final IRBuilder builder) {
        LLVMDeleteBasicBlock(builder.getBlockAddress());
        final var entries = builders.entrySet();
        for(final var entry : entries) {
            if(entry.getValue() != builder) {
                continue;
            }
            builders.remove(entry.getKey());
            break;
        }
    }
}

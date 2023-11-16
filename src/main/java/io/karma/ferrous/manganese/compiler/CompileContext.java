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

package io.karma.ferrous.manganese.compiler;

import io.karma.ferrous.manganese.compiler.pass.CompilePass;
import io.karma.ferrous.manganese.module.Module;
import io.karma.ferrous.manganese.module.ModuleData;
import io.karma.ferrous.manganese.util.TokenUtils;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Alexander Hinze
 * @since 19/10/2023
 */
@API(status = Status.STABLE)
public final class CompileContext implements AutoCloseable {
    private final ConcurrentLinkedQueue<CompileError> errors = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Module> modules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ModuleData> moduleData = new ConcurrentHashMap<>();
    private final AtomicReference<CompileStatus> status = new AtomicReference<>(CompileStatus.SUCCESS);
    private final ThreadLocal<ThreadLocals> threadLocals = ThreadLocal.withInitial(ThreadLocals::new);

    public void addModule(final Module module) {
        modules.put(module.getName(), module);
    }

    public @Nullable Module getModule(final String name) {
        return modules.get(name);
    }

    public ModuleData getOrCreateModuleData(final String name) {
        return moduleData.computeIfAbsent(name, ModuleData::new);
    }

    public ModuleData getOrCreateModuleData() {
        return getOrCreateModuleData(Objects.requireNonNull(getCurrentModuleName()));
    }

    public CompileResult makeResult() {
        return new CompileResult(getCurrentStatus(), new ArrayList<>(errors));
    }

    public CompileError makeError(final CompileErrorCode errorCode) {
        return new CompileError(null, null, getCurrentPass(), null, getCurrentSourceFile(), errorCode);
    }

    public CompileError makeError(final String text, final CompileErrorCode errorCode) {
        return new CompileError(null, null, getCurrentPass(), text, getCurrentSourceFile(), errorCode);
    }

    public CompileError makeError(final Token token, final CompileErrorCode errorCode) {
        return new CompileError(token,
            TokenUtils.getLineTokens(getOrCreateModuleData().getTokenStream(), token),
            getCurrentPass(),
            null,
            getCurrentSourceFile(),
            errorCode);
    }

    public CompileError makeError(final Token token, final String text, final CompileErrorCode errorCode) {
        return new CompileError(token,
            TokenUtils.getLineTokens(getOrCreateModuleData().getTokenStream(), token),
            getCurrentPass(),
            text,
            getCurrentSourceFile(),
            errorCode);
    }

    public void reportError(final Token token, final String text, final CompileErrorCode errorCode) {
        addError(makeError(token, text, errorCode));
    }

    public void reportError(final Token token, final CompileErrorCode errorCode) {
        addError(makeError(token, errorCode));
    }

    public void reportError(final String text, final CompileErrorCode errorCode) {
        addError(makeError(text, errorCode));
    }

    public void reportError(final CompileErrorCode errorCode) {
        addError(makeError(errorCode));
    }

    public void addError(final CompileError error) {
        final var tokenStream = getOrCreateModuleData().getTokenStream();
        if (tokenStream != null && tokenStream.size() == 0) {
            tokenStream.fill();
        }
        if (errors.contains(error)) {
            return; // Don't report duplicates
        }
        errors.add(error);
        setCurrentStatus(getCurrentStatus().worse(error.getStatus()));
    }

    public @Nullable CompilePass getCurrentPass() {
        return threadLocals.get().currentPass;
    }

    public void setCurrentPass(final CompilePass currentPass) {
        threadLocals.get().currentPass = currentPass;
    }

    public CompileStatus getCurrentStatus() {
        return status.get();
    }

    public void setCurrentStatus(final CompileStatus status) {
        this.status.set(status);
    }

    public @Nullable String getCurrentModuleName() {
        return threadLocals.get().currentModuleName;
    }

    void setCurrentModuleName(final @Nullable String currentName) {
        threadLocals.get().currentModuleName = currentName;
    }

    public @Nullable Path getCurrentSourceFile() {
        return threadLocals.get().currentSourceFile;
    }

    void setCurrentSourceFile(final @Nullable Path currentSourceFile) {
        threadLocals.get().currentSourceFile = currentSourceFile;
    }

    public void walkParseTree(final ParseTreeListener listener) {
        ParseTreeWalker.DEFAULT.walk(listener, getOrCreateModuleData().getFileContext());
    }

    public void dispose() {
        modules.values().forEach(Module::dispose); // Dispose the actual modules
        modules.clear();
        moduleData.clear();
        errors.clear();
    }

    @Override
    public void close() {
        dispose();
    }

    private static final class ThreadLocals {
        private String currentModuleName;
        private Path currentSourceFile;
        private CompilePass currentPass;
    }
}

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

package io.karma.ferrous.manganese.profiler;

import io.karma.ferrous.manganese.util.Logger;
import io.karma.kommons.function.Functions;
import io.karma.kommons.io.DeletingFileVisitor;
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.remotery.Remotery;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 01/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class RemoteryProfiler implements Profiler {
    private Path interfaceRoot;
    private long address;

    private void unpackInterface() {
        try {
            final var sourceUrl = RemoteryProfiler.class.getClassLoader().getResource("vis");
            if (sourceUrl == null) {
                Logger.INSTANCE.warnln("Could not unpack profiler interface: no such resource");
                return;
            }
            final var codeSource = Path.of(RemoteryProfiler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // If this is a directory, we just copy files recursively since we are in a dev environment
            if (Files.isDirectory(codeSource)) {
                // Start directly from the uncompressed build outputs
                interfaceRoot = Path.of(sourceUrl.toURI());
                return;
            }
            try (final var jar = new JarFile(codeSource.toFile(), false)) {
                interfaceRoot = Files.createTempDirectory(Path.of(System.getProperty("user.home")),
                    "manganese").resolve("vis");
                Functions.tryDo(() -> Files.createDirectories(interfaceRoot), Logger.INSTANCE::handleException);
                jar.stream().filter(entry -> !entry.isDirectory() && entry.getName().startsWith("vis")).forEach(entry -> {
                    final var entryName = entry.getName();
                    final var dest = interfaceRoot.resolve(entryName.substring("vis/".length()));
                    Logger.INSTANCE.debugln(STR."Uncompressing \{entryName} to \{dest}");
                    Functions.tryDo(() -> Files.createDirectories(dest.getParent()), Logger.INSTANCE::handleException);
                    try (final var inStream = jar.getInputStream(entry); final var outStream = Files.newOutputStream(
                        dest,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING)) {
                        inStream.transferTo(outStream);
                    }
                    catch (Throwable error) {
                        Logger.INSTANCE.warnln(STR."Could not unpack interface resource: \{error}");
                    }
                });
            }
        }
        catch (Throwable error) {
            Logger.INSTANCE.warnln(STR."Could not unpack profiler interface: \{error}");
        }
    }

    private void removeInterface() {
        if (interfaceRoot == null) {
            return; // Just ignore if it wasn't initialized properly
        }
        Functions.tryDo(() -> {
            Files.walkFileTree(interfaceRoot, DeletingFileVisitor.INSTANCE);
            Files.delete(interfaceRoot.getParent()); // Delete parent directory too
        }, Logger.INSTANCE::handleException);
    }

    @Override
    public void init() {
        unpackInterface();
        try (final var stack = MemoryStack.stackPush()) {
            final var addressBuffer = stack.callocPointer(1);
            Remotery.rmt_CreateGlobalInstance(addressBuffer);
            if ((address = addressBuffer.get()) == NULL) {
                throw new IllegalStateException("Could not allocate profiler instance");
            }
            Desktop.getDesktop().browse(interfaceRoot.resolve("index.html").toAbsolutePath().toUri());
            Thread.sleep(TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS));
        }
        catch (Throwable error) {
            Logger.INSTANCE.warnln(STR."Could not initialize profiler: \{error}");
        }
    }

    @Override
    public void dispose() {
        if (address != NULL) {
            Functions.tryDo(() -> Thread.sleep(TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)),
                Logger.INSTANCE::handleException);
            Remotery.rmt_DestroyGlobalInstance(address);
            address = NULL;
        }
        removeInterface();
    }

    @Override
    public void push() {
        try {
            final var traces = Thread.currentThread().getStackTrace();
            if (traces.length < 3) {
                return;
            }
            final var trace = traces[2];
            push(STR."\{trace.getClassName()}#\{trace.getMethodName()}");
        }
        catch (Throwable error) {
            Logger.INSTANCE.warnln(STR."Could not push profiler section: \{error}");
        }
    }

    @Override
    public void push(final String sectionName) {
        try (final var stack = MemoryStack.stackPush()) {
            final var buffer = stack.UTF8(sectionName, true);
            Remotery.nrmt_BeginCPUSample(MemoryUtil.memAddress(buffer), 0, NULL);
        }
    }

    @Override
    public void pop() {
        Remotery.rmt_EndCPUSample();
    }
}



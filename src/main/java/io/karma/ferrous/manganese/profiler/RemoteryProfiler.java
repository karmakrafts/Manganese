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
import org.apiguardian.api.API;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.remotery.Remotery;

import java.awt.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * @author Alexander Hinze
 * @since 01/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class RemoteryProfiler implements Profiler {
    private long address;

    @Override
    public void init() {
        try (final var stack = MemoryStack.stackPush()) {
            final var addressBuffer = stack.callocPointer(1);
            Remotery.rmt_CreateGlobalInstance(addressBuffer);
            if ((address = addressBuffer.get()) == NULL) {
                throw new IllegalStateException("Could not allocate profiler instance");
            }
            Desktop.getDesktop().browse(Path.of("vis/index.html").toAbsolutePath().toUri());
            Thread.sleep(TimeUnit.MILLISECONDS.convert(6, TimeUnit.SECONDS));
        }
        catch (Throwable error) {
            Logger.INSTANCE.warn("Could not initialize profiler: %s", error);
        }
    }

    @Override
    public void dispose() {
        if (address != NULL) {
            Remotery.rmt_DestroyGlobalInstance(address);
            address = NULL;
        }
    }

    @Override
    public void push() {
        try {
            final var traces = Thread.currentThread().getStackTrace();
            if (traces.length < 3) {
                return;
            }
            final var trace = traces[2];
            push(String.format("%s#%s", trace.getClassName(), trace.getMethodName()));
        }
        catch (Throwable error) {
            Logger.INSTANCE.warn("Could not push profiler section: %s", error);
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



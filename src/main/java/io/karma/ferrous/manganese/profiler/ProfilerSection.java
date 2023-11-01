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

import org.apiguardian.api.API;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Hinze
 * @since 01/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class ProfilerSection {
    private final String name;
    private final int level;
    private final long threadId;
    private final long startTime;
    private long endTime;

    ProfilerSection(final String name, final int level) {
        this.name = name;
        this.level = level;
        threadId = Thread.currentThread().threadId();
        startTime = System.nanoTime();
    }

    void setEndTime() {
        endTime = System.nanoTime();
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getTime(final TimeUnit unit) {
        return unit.convert(endTime - startTime, TimeUnit.NANOSECONDS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, threadId, startTime, endTime);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ProfilerSection section) { // @formatter:off
            return name.equals(section.name)
                && threadId == section.threadId
                && startTime == section.startTime
                && endTime == section.endTime;
        } // @formatter:on
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s (%dms)", name, getTime(TimeUnit.MILLISECONDS));
    }
}

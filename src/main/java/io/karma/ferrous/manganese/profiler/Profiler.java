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
import org.barfuin.texttree.api.DefaultNode;
import org.barfuin.texttree.api.TextTree;
import org.barfuin.texttree.api.TreeOptions;
import org.barfuin.texttree.api.style.TreeStyles;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexander Hinze
 * @since 01/11/2023
 */
@API(status = API.Status.INTERNAL)
public final class Profiler {
    public static final Profiler INSTANCE = new Profiler();
    private final ThreadLocal<Stack<ProfilerSection>> sectionStack = ThreadLocal.withInitial(Stack::new);
    private final ConcurrentHashMap<Long, ArrayList<ProfilerSection>> sections = new ConcurrentHashMap<>();

    // @formatter:off
    private Profiler() {}
    // @formatter:on

    public void reset() {
        sections.clear();
    }

    public void push(final String name) {
        try {
            final var traceElements = Thread.currentThread().getStackTrace();
            if (traceElements.length < 3) {
                throw new IllegalStateException("Cannot create trace for profiler section");
            }
            final var element = traceElements[2];
            final var stack = sectionStack.get();
            final var text = String.format("%s:%d#%s - %s",
                element.getFileName(),
                element.getLineNumber(),
                element.getMethodName(),
                name);
            stack.push(new ProfilerSection(text, stack.size()));
        }
        catch (Exception error) {
            Logger.INSTANCE.errorln("Could not push profiler section: %s", error.getMessage());
        }
    }

    public void push() {
        try {
            final var traceElements = Thread.currentThread().getStackTrace();
            if (traceElements.length < 3) {
                throw new IllegalStateException("Cannot create trace for profiler section");
            }
            final var element = traceElements[2];
            final var stack = sectionStack.get();
            final var text = String.format("%s:%d#%s",
                element.getFileName(),
                element.getLineNumber(),
                element.getMethodName());
            stack.push(new ProfilerSection(text, stack.size()));
        }
        catch (Exception error) {
            Logger.INSTANCE.errorln("Could not push profiler section: %s", error.getMessage());
        }
    }

    public void pop() {
        final var section = sectionStack.get().pop();
        section.setEndTime(); // Update end-time when popping a section
        sections.computeIfAbsent(section.getThreadId(), id -> new ArrayList<>()).add(section);
    }

    public long[] getRecordedThreads() {
        final var result = new long[sections.size()];
        final var keys = sections.keySet();
        var index = 0;
        for (final var key : keys) {
            result[index++] = key;
        }
        return result;
    }

    public List<ProfilerSection> getSections(final long threadId) {
        if (!sections.containsKey(threadId)) {
            return Collections.emptyList();
        }
        return sections.get(threadId);
    }

    public String renderSection(final long threadId) {
        final var node = new DefaultNode(String.format("Thread %d", threadId));
        for (final var section : getSections(threadId)) {
            final var time = section.getTime(TimeUnit.MILLISECONDS);
            var color = Ansi.Color.BLUE;
            if (time > 2) {
                color = Ansi.Color.GREEN;
            }
            if (time > 20) {
                color = Ansi.Color.YELLOW;
            }
            if (time > 200) {
                color = Ansi.Color.RED;
            }
            // @formatter:off
            node.addChild(new DefaultNode(Ansi.ansi()
                .fg(color)
                .a(section)
                .a(Ansi.Attribute.RESET)
                .toString()));
            // @formatter:on
        }
        final var options = new TreeOptions();
        options.setStyle(TreeStyles.UNICODE_ROUNDED);
        return TextTree.newInstance(options).render(node);
    }

    public String renderSections() {
        final var builder = new StringBuilder();
        final var threadIds = getRecordedThreads();
        final var numThreads = threadIds.length;
        for (var i = 0; i < numThreads; i++) {
            final var threadId = threadIds[i];
            builder.append(renderSection(threadId));
            if (i < numThreads - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}



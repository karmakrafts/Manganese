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

package io.karma.ferrous.manganese.util;

import io.karma.kommons.util.ArrayUtils;
import io.karma.kommons.util.SystemInfo;
import it.unimi.dsi.fastutil.chars.CharPredicate;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.jetbrains.annotations.Nullable;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.IntFunction;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class KitchenSink {
    // @formatter:off
    private KitchenSink() {}
    // @formatter:on

    public static String consumeCharsUntil(final String value, final CharPredicate predicate) {
        final var builder = new StringBuilder();
        final var length = value.length();
        for (var i = 0; i < length; i++) {
            final var currentChar = value.charAt(i);
            if (!predicate.test(currentChar)) {
                break;
            }
            builder.append(currentChar);
        }
        return builder.toString();
    }

    public static String consumeCharsUntil(final String value, final char... chars) {
        return consumeCharsUntil(value, c -> ArrayUtils.contains(chars, c));
    }

    public static boolean areTypesAssignable(final List<?> objects, final Class<?>... types) {
        final var numObjects = objects.size();
        if (numObjects != types.length) {
            return false;
        }
        for (var i = 0; i < numObjects; i++) {
            if (types[i].isAssignableFrom(objects.get(i).getClass())) {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean containsAssignableType(final List<?> objects, final Class<?> type) {
        for (final var obj : objects) {
            if (!type.isAssignableFrom(obj.getClass())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static boolean startsWithAssignableTypes(final List<?> objects, final Class<?>... types) {
        final var numObjects = objects.size();
        if (numObjects < types.length) {
            return false;
        }
        for (var i = 0; i < numObjects; i++) {
            if (!types[i].isAssignableFrom(objects.get(i).getClass())) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static boolean containsAssignableTypeSequence(final List<?> objects, final Class<?>... types) {
        final var numObjects = objects.size();
        final var numTypes = types.length;
        outer:
        for (var start = 0; start <= numObjects - numTypes; start++) {
            for (var i = 0; i < numTypes; i++) {
                final var objType = objects.get(start + i).getClass();
                final var type = types[i];
                if (!type.isAssignableFrom(objType)) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public static <E extends Enum<E>> void printAvailableValues(final Class<E> type, final String message) {
        Logger.INSTANCE.infoln(message);
        final var values = type.getEnumConstants();
        for (final var value : values) {
            Logger.INSTANCE.infoln("  - %s", value);
        }
    }

    public static <E extends Enum<E>> EnumSet<E> of(final IntFunction<E[]> arrayFactory, final E... values) {
        final var remaining = ArrayUtils.slice(values, 1, values.length - 1, arrayFactory);
        return EnumSet.of(values[0], remaining);
    }

    public static <E extends Enum<E>> EnumSet<E> allExcept(final Class<E> type, final E... excluded) {
        final var values = type.getEnumConstants();
        final var result = EnumSet.allOf(type);
        for (final var value : excluded) {
            result.remove(value);
        }
        return result;
    }

    private static String getSystemShell() {
        if (SystemInfo.Platform.getCurrent() == SystemInfo.Platform.WINDOWS) {
            return "cmd.exe /C";
        }
        return "/bin/sh -c";
    }

    public static ProcessBuilder createProcess(final String... command) {
        Logger.INSTANCE.debugln("Spawning process: %s", String.join(" ", command));
        final var builder = new ProcessBuilder(getSystemShell()).command(command);
        builder.environment().putAll(System.getenv());
        return builder;
    }

    public static boolean hasCommand(final String... command) {
        try {
            return createProcess(command).start().waitFor() == 0;
        }
        catch (Exception error) {
            return false;
        }
    }

    public static List<Path> findFilesWithExtensions(final Path path, final String... extensions) {
        final var files = new ArrayList<Path>();
        if (!Files.isDirectory(path)) {
            files.add(path);
            return files;
        }
        try {
            Files.walkFileTree(path, new SimpleFileVisitor(filePath -> {
                final var fileName = filePath.getFileName().toString();
                for (final var ext : extensions) {
                    if (!fileName.endsWith(String.format(".%s", ext))) {
                        continue;
                    }
                    files.add(filePath);
                    break;
                }
                return FileVisitResult.CONTINUE;
            }));
        }
        catch (Exception error) { /* swallow exception */ }
        return files;
    }

    public static String getRawFileName(final Path path) {
        final var fileName = path.getFileName().toString();
        if (!fileName.contains(".")) {
            return fileName;
        }
        final var lastDot = fileName.lastIndexOf('.');
        return fileName.substring(0, lastDot);
    }

    public static Path derivePathExtension(final Path path, final String ext) {
        return path.getParent().resolve(String.format("%s.%s", getRawFileName(path), ext));
    }

    public static String capitalize(final String value) {
        var result = value.substring(0, 1).toUpperCase();
        result += value.substring(1);
        return result;
    }

    public static String makeCompilerMessage(final String message, final Color color,
                                             final @Nullable List<String> values) {
        final var builder = Ansi.ansi();
        builder.fgBright(color);
        builder.a(message);
        if (values != null) {
            builder.a(":\n");
            final var numValues = values.size();
            for (var i = 0; i < numValues; i++) {
                builder.fgBright(Color.BLUE);
                builder.a(values.get(i));
                builder.fgBright(color);
                if (i < numValues - 1) {
                    builder.a(", ");
                }
            }
        }
        builder.a(Attribute.RESET);
        return builder.toString();
    }

    public static String makeCompilerMessage(final String message, final @Nullable List<String> values) {
        return makeCompilerMessage(message, Color.RED, values);
    }

    public static String makeCompilerMessage(final String message) {
        return makeCompilerMessage(message, Color.RED, null);
    }

    public static String getProgressIndicator(final int numFiles, final int index) {
        final var percent = (int) (((float) index / (float) numFiles) * 100F);
        final var str = percent + "%%";
        final var length = str.length();
        final var builder = new StringBuilder();

        while (builder.length() < (5 - length)) {
            builder.append(' ');
        }

        builder.insert(0, '[');
        builder.append(str);
        builder.append(']');
        return builder.toString();
    }
}

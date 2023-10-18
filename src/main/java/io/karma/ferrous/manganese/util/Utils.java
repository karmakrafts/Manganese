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

import io.karma.ferrous.vanadium.FerrousParser.IdentContext;
import io.karma.ferrous.vanadium.FerrousParser.LerpIdentContext;
import io.karma.ferrous.vanadium.FerrousParser.QualifiedIdentContext;
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
import java.util.List;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
@API(status = Status.INTERNAL)
public final class Utils {
    // @formatter:off
    private Utils() {}
    // @formatter:on

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

    public static Identifier getIdentifier(final IdentContext context) {
        final var children = context.children;
        final var buffer = new StringBuilder();
        for (final var child : children) {
            if (child instanceof LerpIdentContext lerpContext) {
                // TODO: handle interpolated identifiers
                Logger.INSTANCE.warn("Identifier interpolation is not implemented right now");
                continue;
            }
            buffer.append(child.getText());
        }
        return Identifier.parse(buffer.toString());
    }

    public static Identifier getIdentifier(final QualifiedIdentContext context) {
        final var children = context.children;
        final var buffer = new StringBuilder();
        for (final var child : children) {
            if (child instanceof LerpIdentContext lerpContext) {
                // TODO: handle interpolated identifiers
                Logger.INSTANCE.warn("Identifier interpolation is not implemented right now");
                continue;
            }
            buffer.append(child.getText());
        }
        return Identifier.parse(buffer.toString());
    }

    public static String getRawFileName(final Path path) {
        final var fileName = path.getFileName().toString();
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
            builder.a(":\n  ");
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

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

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

import java.util.List;

/**
 * @author Alexander Hinze
 * @since 13/10/2023
 */
public final class Utils {
    // @formatter:off
    private Utils() {}
    // @formatter:on

    public static String makeSuggestion(final String message, final List<String> values) {
        final var builder = Ansi.ansi();
        builder.a("  ");
        builder.fgBright(Color.BLACK);
        builder.a(message);
        builder.a(":\n  ");
        final var numValues = values.size();
        for (var i = 0; i < numValues; i++) {
            builder.fgBright(Color.BLUE);
            builder.a(values.get(i));
            builder.fgBright(Color.BLACK);
            if (i < numValues - 1) {
                builder.a(", ");
            }
        }
        return builder.toString();
    }
}

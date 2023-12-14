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

import io.karma.kommons.util.ExceptionUtils;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.Nullable;

import java.io.Writer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Simple console-based logging facade using JAnsi.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.INTERNAL)
public final class Logger extends Writer {
    public static final Logger INSTANCE = new Logger();
    private static final String[] LOGO_LINES = { // @formatter:off
        "  __  __                                              ",
        " |  \\/  | __ _ _ __   __ _  __ _ _ __   ___  ___  ___ ",
        " | |\\/| |/ _` | '_ \\ / _` |/ _` | '_ \\ / _ \\/ __|/ _ \\",
        " | |  | | (_| | | | | (_| | (_| | | | |  __/\\__ \\  __/",
        " |_|  |_|\\__,_|_| |_|\\__, |\\__,_|_| |_|\\___||___/\\___|",
        "                     |___/                            "
    }; // @formatter:on

    static {
        if (AnsiConsole.isInstalled()) {
            throw new IllegalStateException("ANSI subsystem already initialized");
        }
        AnsiConsole.systemInstall();
        Runtime.getRuntime().addShutdownHook(new Thread(AnsiConsole::systemUninstall));
    }

    private final Set<LogLevel> activeLevels = Collections.synchronizedSet(EnumSet.allOf(LogLevel.class));
    private LogLevel logLevel = LogLevel.INFO;
    private Consumer<String> logConsumer = System.out::print;

    // @formatter:off
    private Logger() {}
    // @formatter:on

    public <X extends Throwable> void handleException(final X error) {
        ExceptionUtils.handleError(error, this::errorln);
    }

    // Writer overrides

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        infoln(new String(cbuf, off, len));
    }

    // @formatter:off
    @Override
    public void flush() {}

    @Override
    public void close() {}
    // @formatter:on

    // Functions

    public void enableLogLevel(final LogLevel level) {
        activeLevels.add(level);
    }

    public void disableLogLevel(final LogLevel level) {
        activeLevels.remove(level);
    }

    public void setLogLevel(final LogLevel level) {
        logLevel = level;
    }

    public void setLogConsumer(final @Nullable Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    public void logln(final LogLevel level, final String message) {
        if (!activeLevels.contains(level)) {
            return; // Prioritize this condition over the current log level
        }
        if (logConsumer != null && level.ordinal() >= logLevel.ordinal()) {
            logConsumer.accept(STR."\{level.format(message)}\n");
        }
    }

    public void debugln(final String message) {
        logln(LogLevel.DEBUG, message);
    }

    public void infoln(final String message) {
        logln(LogLevel.INFO, message);
    }

    public void warnln(final String message) {
        logln(LogLevel.WARN, message);
    }

    public void errorln(final String message) {
        logln(LogLevel.ERROR, message);
    }

    public void fatalln(final String message) {
        logln(LogLevel.FATAL, message);
    }

    public void log(final LogLevel level, final String message) {
        if (!activeLevels.contains(level)) {
            return; // Prioritize this condition over the current log level
        }
        if (logConsumer != null && level.ordinal() >= logLevel.ordinal()) {
            logConsumer.accept(level.format(message));
        }
    }

    public void debug(final String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(final String message) {
        log(LogLevel.INFO, message);
    }

    public void warn(final String message) {
        log(LogLevel.WARN, message);
    }

    public void error(final String message) {
        log(LogLevel.ERROR, message);
    }

    public void fatal(final String message) {
        log(LogLevel.FATAL, message);
    }

    public void printLogo() {
        infoln(Ansi.ansi().fg(Color.RED).a(LOGO_LINES[0]).a(Attribute.RESET).toString());
        infoln(Ansi.ansi().fgBright(Color.RED).a(LOGO_LINES[1]).a(Attribute.RESET).toString());
        infoln(Ansi.ansi().fgBright(Color.YELLOW).a(LOGO_LINES[2]).a(Attribute.RESET).toString());
        infoln(Ansi.ansi().fg(Color.GREEN).a(LOGO_LINES[3]).a(Attribute.RESET).toString());
        infoln(Ansi.ansi().fg(Color.BLUE).a(LOGO_LINES[4]).a(Attribute.RESET).toString());
        infoln(Ansi.ansi().fg(Color.MAGENTA).a(LOGO_LINES[5]).a(Attribute.RESET).toString());
    }

    public enum LogLevel {
        // @formatter:off
        DEBUG   (Color.DEFAULT, Color.CYAN),
        INFO    (Color.DEFAULT, Color.DEFAULT),
        WARN    (Color.DEFAULT, Color.YELLOW),
        ERROR   (Color.DEFAULT, Color.RED),
        FATAL   (Color.RED, Color.WHITE);
        // @formatter:on

        private final Ansi.Color bgColor;
        private final Ansi.Color fgColor;
        private final Ansi.Attribute[] attribs;

        LogLevel(final Ansi.Color bgColor, final Ansi.Color fgColor, final Ansi.Attribute... attribs) {
            this.bgColor = bgColor;
            this.fgColor = fgColor;
            this.attribs = attribs;
        }

        public String format(final String s) {
            final var builder = Ansi.ansi();
            builder.bg(bgColor);
            builder.fg(fgColor);

            for (final var attrib : attribs) {
                builder.a(attrib);
            }

            builder.a(s);
            builder.a(Attribute.RESET);
            return builder.toString();
        }
    }
}

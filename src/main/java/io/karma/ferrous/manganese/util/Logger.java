package io.karma.ferrous.manganese.util;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.Nullable;

import java.io.Writer;
import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * Simple console-based logging facade using JAnsi.
 *
 * @author Alexander Hinze
 * @since 02/07/2022
 */
@API(status = Status.STABLE)
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

    private final StringBuilder messageBuffer = new StringBuilder();
    private final EnumSet<LogLevel> activeLevels = EnumSet.allOf(LogLevel.class);
    private LogLevel logLevel = LogLevel.INFO;
    private Consumer<String> logConsumer = System.out::println;

    // @formatter:off
    private Logger() {}
    // @formatter:on

    // Writer overrides

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        info(new String(cbuf, off, len));
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

    public void log(final LogLevel level, final String fmt, final Object... params) {
        if (!activeLevels.contains(level)) {
            return; // Prioritize this condition over the current log level
        }

        if (logConsumer != null && level.ordinal() >= logLevel.ordinal()) {
            messageBuffer.delete(0, messageBuffer.length());
            final var formatted = String.format(fmt, params);
            final var lines = formatted.split("\n");
            final var numLines = lines.length;
            final var maxIndex = numLines - 1;

            for (var i = 0; i < numLines; i++) {
                final var line = lines[i];
                messageBuffer.append(level.format(line));

                if (i < maxIndex) {
                    messageBuffer.append('\n');
                }
            }

            logConsumer.accept(messageBuffer.toString());
        }
    }

    public void debug(final String fmt, final Object... params) {
        log(LogLevel.DEBUG, fmt, params);
    }

    public void info(final String fmt, final Object... params) {
        log(LogLevel.INFO, fmt, params);
    }

    public void warn(final String fmt, final Object... params) {
        log(LogLevel.WARN, fmt, params);
    }

    public void error(final String fmt, final Object... params) {
        log(LogLevel.ERROR, fmt, params);
    }

    public void fatal(final String fmt, final Object... params) {
        log(LogLevel.FATAL, fmt, params);
    }

    public void printLogo() {
        info(Ansi.ansi().fg(Color.RED).a(LOGO_LINES[0]).a(Attribute.RESET).toString());
        info(Ansi.ansi().fgBright(Color.RED).a(LOGO_LINES[1]).a(Attribute.RESET).toString());
        info(Ansi.ansi().fgBright(Color.YELLOW).a(LOGO_LINES[2]).a(Attribute.RESET).toString());
        info(Ansi.ansi().fg(Color.GREEN).a(LOGO_LINES[3]).a(Attribute.RESET).toString());
        info(Ansi.ansi().fg(Color.BLUE).a(LOGO_LINES[4]).a(Attribute.RESET).toString());
        info(Ansi.ansi().fg(Color.MAGENTA).a(LOGO_LINES[5]).a(Attribute.RESET).toString());
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

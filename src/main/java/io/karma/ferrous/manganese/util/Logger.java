package io.karma.ferrous.manganese.util;

import io.karma.ferrous.manganese.ManganeseCompiler;
import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Attribute;
import org.fusesource.jansi.Ansi.Color;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Writer;
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
    private final StringBuilder messageBuffer = new StringBuilder();
    private boolean isInitialized;
    private LogLevel logLevel = LogLevel.INFO;
    private Consumer<String> logConsumer = System.out::println;

    // @formatter:off
    private Logger() {}
    // @formatter:on

    // Writer overrides

    @Override
    public void write(final char @NotNull [] cbuf, final int off, final int len) {
        info(new String(cbuf, off, len));
    }

    // @formatter:off
    @Override
    public void flush() {}

    @Override
    public void close() {}
    // @formatter:on

    // Functions

    public void setLogLevel(final @NotNull LogLevel level) {
        logLevel = level;
    }

    public void setLogConsumer(final @Nullable Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
    }

    @API(status = Status.INTERNAL)
    public void init() {
        if (isInitialized) {
            return;
        }

        isInitialized = true;

        if (ManganeseCompiler.isEmbedded()) {
            return; // No action for the embedded compiler
        }

        if (AnsiConsole.isInstalled()) {
            throw new IllegalStateException("ANSI subsystem already initialized");
        }

        AnsiConsole.systemInstall();
        Runtime.getRuntime().addShutdownHook(new Thread(AnsiConsole::systemUninstall));
    }

    public void log(final @NotNull LogLevel level, final @NotNull String fmt, final Object... params) {
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

    public void debug(final @NotNull String fmt, final Object... params) {
        log(LogLevel.DEBUG, fmt, params);
    }

    public void info(final @NotNull String fmt, final Object... params) {
        log(LogLevel.INFO, fmt, params);
    }

    public void warn(final @NotNull String fmt, final Object... params) {
        log(LogLevel.WARN, fmt, params);
    }

    public void error(final @NotNull String fmt, final Object... params) {
        log(LogLevel.ERROR, fmt, params);
    }

    public void fatal(final @NotNull String fmt, final Object... params) {
        log(LogLevel.FATAL, fmt, params);
    }

    public enum LogLevel {
        // @formatter:off
        DEBUG   (Color.DEFAULT, Color.CYAN),
        INFO    (Color.DEFAULT, Color.DEFAULT),
        WARN    (Color.YELLOW, Color.BLACK),
        ERROR   (Color.DEFAULT, Color.RED),
        FATAL   (Color.RED, Color.WHITE);
        // @formatter:on

        private final Ansi.Color bgColor;
        private final Ansi.Color fgColor;
        private final Ansi.Attribute[] attribs;

        LogLevel(final @NotNull Ansi.Color bgColor, final @NotNull Ansi.Color fgColor, final Ansi.Attribute... attribs) {
            this.bgColor = bgColor;
            this.fgColor = fgColor;
            this.attribs = attribs;
        }

        public @NotNull String format(final @NotNull String s) {
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

package org.tauasa.faux.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Whether the operating system is set to dark. JavaFX 21, the last line that
 * runs on Java 17, can't say, so this asks the system the way each one
 * stores it:
 *
 * <ul>
 * <li>macOS: {@code defaults read -g AppleInterfaceStyle} prints {@code Dark},
 * and fails when the system is light</li>
 * <li>Windows: {@code AppsUseLightTheme} is {@code 0x0} under
 * {@code HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize}</li>
 * <li>Linux: GNOME's {@code color-scheme} is {@code prefer-dark}, or its GTK
 * theme's name says dark</li>
 * </ul>
 *
 * Anything it can't read counts as light. Each question runs a short command,
 * so call it off the FX thread.
 */
@Component
public class SystemTheme {

    private static final long TIMEOUT_SECONDS = 2;

    private final Function<List<String>, Optional<String>> run;
    private final String osName;

    @Autowired
    public SystemTheme() {
        this(SystemTheme::runCommand, System.getProperty("os.name", ""));
    }

    /**
     * @param run    runs a command and gives its output, or empty if it failed or couldn't run
     * @param osName the {@code os.name} to answer for
     */
    SystemTheme(Function<List<String>, Optional<String>> run, String osName) {
        this.run = run;
        this.osName = osName.toLowerCase(Locale.ROOT);
    }

    private volatile Boolean lastKnown;

    /**
     * The last answer {@link #isDark()} gave, asking now only if it never has.
     * Cheap after the first call, so the window can use it as it opens.
     */
    public boolean lastKnownIsDark() {
        Boolean known = lastKnown;
        return known != null ? known : isDark();
    }

    /** Asks the system now whether it's set to dark, and remembers the answer. */
    public boolean isDark() {
        boolean dark = ask();
        lastKnown = dark;
        return dark;
    }

    private boolean ask() {
        if (osName.startsWith("mac")) {
            return run.apply(List.of("defaults", "read", "-g", "AppleInterfaceStyle"))
                    .map(out -> out.strip().equalsIgnoreCase("dark")).orElse(false);
        }
        if (osName.startsWith("windows")) {
            return run.apply(List.of("reg", "query",
                            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                            "/v", "AppsUseLightTheme"))
                    .map(out -> out.matches("(?s).*AppsUseLightTheme\\s+REG_DWORD\\s+0x0+\\b.*")).orElse(false);
        }
        Optional<String> scheme = run.apply(List.of("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"));
        if (scheme.map(s -> s.contains("prefer-dark")).orElse(false)) {
            return true;
        }
        return run.apply(List.of("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"))
                .map(s -> s.toLowerCase(Locale.ROOT).contains("dark")).orElse(false);
    }

    /** Output of a command that exited 0 within the timeout; empty otherwise. */
    static Optional<String> runCommand(List<String> command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return process.exitValue() == 0 ? Optional.of(out) : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }
}

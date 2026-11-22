package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Appearance, the system light/dark check, and the file the choice is kept in. */
class AppearanceTest {

    @TempDir
    Path dir;

    @Test
    void onlyDarkAndSystemOnADarkOsShowDark() {
        assertTrue(Appearance.DARK.isDark(false));
        assertTrue(Appearance.SYSTEM.isDark(true));
        assertFalse(Appearance.SYSTEM.isDark(false));
        assertFalse(Appearance.LIGHT.isDark(true));
    }

    @Test
    void parsesStoredNamesLeniently() {
        assertEquals(Optional.of(Appearance.DARK), Appearance.parse(" dark\n"));
        assertEquals(Optional.of(Appearance.SYSTEM), Appearance.parse("SYSTEM"));
        assertTrue(Appearance.parse("sepia").isEmpty());
        assertTrue(Appearance.parse(null).isEmpty());
    }

    @Test
    void theDarkStylesheetShipsAndSetsTheBaseColors() throws IOException {
        assertNotNull(Appearance.DARK_STYLESHEET);
        try (InputStream in = URI.create(Appearance.DARK_STYLESHEET).toURL().openStream()) {
            String css = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(css.contains("-fx-base:") && css.contains("-fx-background:"), css);
        }
    }

    @Test
    void macReadsAppleInterfaceStyle() {
        List<List<String>> asked = new ArrayList<>();
        SystemTheme dark = new SystemTheme(cmd -> {
            asked.add(cmd);
            return Optional.of("Dark\n");
        }, "Mac OS X");
        assertTrue(dark.isDark());
        assertEquals(List.of("defaults", "read", "-g", "AppleInterfaceStyle"), asked.get(0));
        // In light mode the key doesn't exist and defaults exits 1.
        assertFalse(new SystemTheme(cmd -> Optional.empty(), "Mac OS X").isDark());
    }

    @Test
    void windowsReadsAppsUseLightTheme() {
        String darkOut = "\r\nHKEY_CURRENT_USER\\...\\Personalize\r\n    AppsUseLightTheme    REG_DWORD    0x0\r\n";
        String lightOut = "\r\nHKEY_CURRENT_USER\\...\\Personalize\r\n    AppsUseLightTheme    REG_DWORD    0x1\r\n";
        assertTrue(new SystemTheme(cmd -> Optional.of(darkOut), "Windows 11").isDark());
        assertFalse(new SystemTheme(cmd -> Optional.of(lightOut), "Windows 11").isDark());
        assertFalse(new SystemTheme(cmd -> Optional.empty(), "Windows 10").isDark());
    }

    @Test
    void linuxReadsGnomeColorSchemeThenTheGtkTheme() {
        assertTrue(new SystemTheme(cmd -> cmd.contains("color-scheme")
                ? Optional.of("'prefer-dark'\n") : Optional.empty(), "Linux").isDark());
        assertTrue(new SystemTheme(cmd -> cmd.contains("color-scheme")
                ? Optional.of("'default'\n") : Optional.of("'Adwaita-dark'\n"), "Linux").isDark());
        assertFalse(new SystemTheme(cmd -> cmd.contains("color-scheme")
                ? Optional.of("'default'\n") : Optional.of("'Adwaita'\n"), "Linux").isDark());
        assertFalse(new SystemTheme(cmd -> Optional.empty(), "Linux").isDark());
    }

    @Test
    void lastKnownAsksOnceThenRemembers() {
        int[] calls = {0};
        SystemTheme theme = new SystemTheme(cmd -> {
            calls[0]++;
            return Optional.of("Dark");
        }, "Mac OS X");
        assertTrue(theme.lastKnownIsDark());
        assertTrue(theme.lastKnownIsDark());
        assertEquals(1, calls[0]);
        theme.isDark();
        assertEquals(2, calls[0], "isDark should ask again");
    }

    @Test
    void aCommandThatFailsOrDoesntExistIsEmpty() {
        assertTrue(SystemTheme.runCommand(List.of("faux-person-fx-no-such-command")).isEmpty());
        assertTrue(SystemTheme.runCommand(List.of("sh", "-c", "exit 3")).isEmpty());
        assertEquals(Optional.of("hi\n"), SystemTheme.runCommand(List.of("sh", "-c", "echo hi")));
    }

    @Test
    void theChoiceIsSavedAndLoadedAndAnythingElseMeansSystem() throws IOException {
        AppearanceFile file = new AppearanceFile(dir.resolve("nested/appearance.txt"));
        assertEquals(Appearance.SYSTEM, file.load(), "a missing file should mean System");
        file.save(Appearance.DARK);
        assertEquals(Appearance.DARK, file.load());
        Files.writeString(dir.resolve("nested/appearance.txt"), "chartreuse");
        assertEquals(Appearance.SYSTEM, file.load());
    }

    @Test
    void theSplashReadsTheSystemPropertyOrTheDefault() throws IOException {
        Path path = dir.resolve("appearance.txt");
        Files.writeString(path, "LIGHT\n");
        String before = System.getProperty("faux.fx.appearance-file");
        try {
            System.setProperty("faux.fx.appearance-file", path.toString());
            assertEquals(Appearance.LIGHT, AppearanceFile.forSplash());
            System.setProperty("faux.fx.appearance-file", "");
            assertEquals(Appearance.SYSTEM, AppearanceFile.forSplash(), "blank means none is kept");
        } finally {
            if (before == null) {
                System.clearProperty("faux.fx.appearance-file");
            } else {
                System.setProperty("faux.fx.appearance-file", before);
            }
        }
    }
}

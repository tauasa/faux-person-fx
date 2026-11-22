package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.application.Preloader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The splash: shows progress, never runs backwards, and closes only when told the window is up. Needs a display. */
class SplashPreloaderTest {

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        Fx.start();
    }

    @Test
    void showsProgressAndClosesOnReady() throws Exception {
        SplashPreloader splash = new SplashPreloader();
        Fx.run(() -> splash.start(new Stage()));
        assertTrue(Fx.call(() -> splash.stage().isShowing()));

        Fx.run(() -> splash.handleApplicationNotification(new Splash.Progress(0.4, "Reading settings")));
        assertEquals(0.4, Fx.call(() -> bar(splash).getProgress()), 1e-9);
        assertEquals("Reading settings", Fx.call(() -> message(splash).getText()));

        Fx.run(() -> splash.handleApplicationNotification(new Splash.Progress(0.2, "Late")));
        assertEquals(0.4, Fx.call(() -> bar(splash).getProgress()), 1e-9, "the bar went backwards");

        // BEFORE_START comes before the window paints; the splash must still be up.
        Fx.run(() -> splash.handleStateChangeNotification(
                new Preloader.StateChangeNotification(Preloader.StateChangeNotification.Type.BEFORE_START)));
        assertTrue(Fx.call(() -> splash.stage().isShowing()), "closed before the window was up");

        Fx.run(() -> splash.handleApplicationNotification(new Splash.Ready()));
        assertFalse(Fx.call(() -> splash.stage().isShowing()));
        assertEquals(1.0, Fx.call(() -> bar(splash).getProgress()), 1e-9);
    }

    @Test
    void theSplashOpensInTheSavedAppearance(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        java.nio.file.Path file = dir.resolve("appearance.txt");
        java.nio.file.Files.writeString(file, "DARK\n");
        String before = System.getProperty("faux.fx.appearance-file");
        System.setProperty("faux.fx.appearance-file", file.toString());
        try {
            SplashPreloader dark = new SplashPreloader();
            Fx.run(() -> dark.start(new Stage()));
            assertTrue(Fx.call(() -> dark.stage().getScene().getStylesheets().contains(Appearance.DARK_STYLESHEET)));
            Fx.run(() -> dark.stage().hide());

            java.nio.file.Files.writeString(file, "LIGHT\n");
            SplashPreloader light = new SplashPreloader();
            Fx.run(() -> light.start(new Stage()));
            assertFalse(Fx.call(() -> light.stage().getScene().getStylesheets().contains(Appearance.DARK_STYLESHEET)));
            Fx.run(() -> light.stage().hide());
        } finally {
            if (before == null) {
                System.clearProperty("faux.fx.appearance-file");
            } else {
                System.setProperty("faux.fx.appearance-file", before);
            }
        }
    }

    @Test
    void aFailedStartClosesTheSplash() throws Exception {
        SplashPreloader splash = new SplashPreloader();
        Fx.run(() -> splash.start(new Stage()));
        Fx.run(() -> splash.handleErrorNotification(
                new Preloader.ErrorNotification(null, "boom", new RuntimeException("boom"))));
        assertFalse(Fx.call(() -> splash.stage().isShowing()));
    }

    private static ProgressBar bar(SplashPreloader splash) {
        return (ProgressBar) splash.stage().getScene().lookup("#splashProgress");
    }

    private static Label message(SplashPreloader splash) {
        return (Label) splash.stage().getScene().lookup("#splashMessage");
    }
}

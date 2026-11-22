package org.tauasa.faux.app;

import javafx.application.Preloader;

/** What {@link FauxFxApp} tells the {@link SplashPreloader} while it starts. */
final class Splash {

    private Splash() {
    }

    /**
     * How far startup has got.
     *
     * @param fraction from 0 to 1
     * @param message  what's happening now, for the line under the bar
     */
    record Progress(double fraction, String message) implements Preloader.PreloaderNotification {
    }

    /** The main window is up and painted: the splash can go. */
    record Ready() implements Preloader.PreloaderNotification {
    }
}

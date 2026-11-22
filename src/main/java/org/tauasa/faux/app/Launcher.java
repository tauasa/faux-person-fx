package org.tauasa.faux.app;

import javafx.application.Application;

/**
 * The jar's main class. JavaFX refuses to start from the classpath when the
 * main class itself extends {@link Application}, so this one doesn't. It
 * also names the splash screen, unless {@code -Djavafx.preloader} already does.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        if (System.getProperty("javafx.preloader") == null) {
            System.setProperty("javafx.preloader", SplashPreloader.class.getName());
        }
        Application.launch(FauxFxApp.class, args);
    }
}

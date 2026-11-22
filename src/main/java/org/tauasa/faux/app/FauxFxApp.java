package org.tauasa.faux.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * The JavaFX application: starts Spring and loads faux-person's data behind
 * the splash screen, then shows the window Spring built and closes the splash.
 */
public class FauxFxApp extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(String[]::new);
        context = new SpringApplicationBuilder(FauxFxApplication.class)
                .headless(false)
                .web(WebApplicationType.NONE)
                .listeners(new StartupProgress(this::notifyPreloader))
                .run(args);

        // faux-person reads its data files on first use. Doing it here keeps
        // the first Generate quick, and the splash shows the wait.
        context.getBean(FauxService.class).warmUp();
        // Ask the system for light or dark now, so the window opens in it
        // without running a command on the FX thread.
        context.getBean(SystemTheme.class).isDark();
        notifyPreloader(new Splash.Progress(0.9, "Building the window\u2026"));
    }

    @Override
    public void start(Stage stage) {
        context.getBean(MainView.class).show(stage, getHostServices()::showDocument);
        // Close the splash on the next pulse, once the window has painted.
        Platform.runLater(() -> notifyPreloader(new Splash.Ready()));
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
        }
    }
}

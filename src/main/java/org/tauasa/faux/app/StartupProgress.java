package org.tauasa.faux.app;

import java.util.function.Consumer;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Turns Spring Boot's startup events into steps on the splash screen's bar.
 * Spring covers the first part of the bar; loading faux-person's data and
 * building the window cover the rest, from {@link FauxFxApp}.
 */
final class StartupProgress implements ApplicationListener<ApplicationEvent> {

    /** Where the bar stands once Spring is ready. */
    static final double SPRING_READY = 0.6;

    private final Consumer<Splash.Progress> sink;

    StartupProgress(Consumer<Splash.Progress> sink) {
        this.sink = sink;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        Splash.Progress step = stepFor(event);
        if (step != null) {
            sink.accept(step);
        }
    }

    static Splash.Progress stepFor(ApplicationEvent event) {
        if (event instanceof ApplicationStartingEvent) {
            return new Splash.Progress(0.05, "Starting…");
        } else if (event instanceof ApplicationEnvironmentPreparedEvent) {
            return new Splash.Progress(0.15, "Reading settings…");
        } else if (event instanceof ApplicationContextInitializedEvent) {
            return new Splash.Progress(0.25, "Creating the application context…");
        } else if (event instanceof ApplicationPreparedEvent) {
            return new Splash.Progress(0.35, "Creating components…");
        } else if (event instanceof ContextRefreshedEvent) {
            return new Splash.Progress(0.5, "Components ready");
        } else if (event instanceof ApplicationStartedEvent) {
            return new Splash.Progress(0.55, "Started");
        } else if (event instanceof ApplicationReadyEvent) {
            return new Splash.Progress(SPRING_READY, "Loading name and place data…");
        }
        return null;
    }
}

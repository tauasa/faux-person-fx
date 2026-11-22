package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** A real Spring start drives the splash's bar forward, step by step, to where the window takes over. */
class StartupProgressTest {

    @Test
    void aRealStartMovesTheBarForwardToSpringReady() {
        List<Splash.Progress> steps = new CopyOnWriteArrayList<>();
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(FauxFxApplication.class)
                .web(WebApplicationType.NONE)
                .listeners(new StartupProgress(steps::add))
                .run()) {
            assertTrue(steps.size() >= 5, "too few steps: " + steps);
            for (int i = 1; i < steps.size(); i++) {
                assertTrue(steps.get(i).fraction() > steps.get(i - 1).fraction(), "went backwards: " + steps);
            }
            assertTrue(steps.get(0).fraction() < 0.2, steps.get(0).toString());
            assertEquals(StartupProgress.SPRING_READY, steps.get(steps.size() - 1).fraction(), 1e-9);
            assertTrue(steps.stream().allMatch(s -> !s.message().isBlank()));
        }
    }
}

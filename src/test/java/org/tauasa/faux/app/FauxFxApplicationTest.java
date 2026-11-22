package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The context starts, builds no window until asked, and reads its settings. */
@SpringBootTest
class FauxFxApplicationTest {

    @Autowired
    private MainView view;

    @Autowired
    private FauxService service;

    @Autowired
    private FauxFxProperties properties;

    @Autowired
    private About about;

    @Test
    void aboutCarriesTheBuildsVersions() {
        // The pom hands surefire its own versions, so a version bump can't break this.
        assertEquals(System.getProperty("expected.version"), about.version());
        assertEquals(System.getProperty("expected.faux-person.version"), about.fauxPersonVersion());
    }

    @Test
    void contextStartsAndReadsSettings() {
        assertNotNull(view);
        assertEquals(100, properties.defaultCount());
        assertEquals(10000, properties.maxCount());
        assertEquals(10000, service.maxCount());
        assertEquals(java.nio.file.Path.of(System.getProperty("user.home"), ".faux-person-fx", "seed-history.txt"),
                properties.historyPath().orElseThrow());
    }
}

package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tauasa.faux.model.Region;

class SeedHistoryTest {

    @Test
    void mostRecentFirstAndOneEntryPerSeed() {
        SeedHistory history = new SeedHistory();
        history.add(new SeedHistory.Run(1, 10, null, false));
        history.add(new SeedHistory.Run(2, 10, null, false));
        history.add(new SeedHistory.Run(3, 10, null, false));
        assertEquals(List.of(3L, 2L, 1L), seeds(history));

        // Running seed 1 again, with other settings, moves it up and keeps the new settings.
        history.add(new SeedHistory.Run(1, 50, Region.JPN, true));
        assertEquals(List.of(1L, 3L, 2L), seeds(history));
        assertEquals(new SeedHistory.Run(1, 50, Region.JPN, true), history.last().orElseThrow());
    }

    @Test
    void keepsAtMostTheLimitDroppingTheOldest() {
        SeedHistory history = new SeedHistory();
        for (long s = 1; s <= SeedHistory.LIMIT + 5; s++) {
            history.add(new SeedHistory.Run(s, 1, null, false));
        }
        assertEquals(SeedHistory.LIMIT, history.runs().size());
        assertEquals(SeedHistory.LIMIT + 5L, history.runs().get(0).seed());
        assertEquals(6L, history.runs().get(SeedHistory.LIMIT - 1).seed());
    }

    @Test
    void findsARunByItsSeedText() {
        SeedHistory history = new SeedHistory();
        history.add(new SeedHistory.Run(-42, 7, Region.BRA, false));
        assertEquals(7, history.find(" -42 ").orElseThrow().count());
        assertTrue(history.find("43").isEmpty());
        assertTrue(history.find("").isEmpty());
        assertTrue(history.find("junk").isEmpty());
        assertTrue(new SeedHistory().last().isEmpty());
    }

    @Test
    void describesARunInTheTermsOfItsKind() {
        assertEquals("25 names, Mixed", new SeedHistory.Run(1, 25, null, true).describe(Kind.NAMES));
        assertEquals("1 person, JPN, real companies",
                new SeedHistory.Run(1, 1, Region.JPN, true).describe(Kind.PEOPLE));
        assertEquals("8 companies, real", new SeedHistory.Run(1, 8, Region.JPN, true).describe(Kind.COMPANIES));
        assertEquals("8 companies", new SeedHistory.Run(1, 8, null, false).describe(Kind.COMPANIES));
    }

    private static List<Long> seeds(SeedHistory history) {
        return history.runs().stream().map(SeedHistory.Run::seed).toList();
    }
}

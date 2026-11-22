package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.tauasa.faux.model.Company;
import org.tauasa.faux.model.Name;
import org.tauasa.faux.model.Person;
import org.tauasa.faux.model.Region;

class FauxServiceTest {

    private final FauxService service = new FauxService(new FauxFxProperties(100, 1000));

    @Test
    void sameSeedGivesSameRowsForEachKind() {
        GenerateRequest request = new GenerateRequest(50, null, 42L, false);
        assertEquals(service.names(request), service.names(request));
        assertEquals(service.companies(request), service.companies(request));
        assertEquals(service.people(request), service.people(request));
        assertEquals(42L, service.people(request).seed());
    }

    @Test
    void differentSeedsGiveDifferentRows() {
        assertNotEquals(
                service.people(new GenerateRequest(20, null, 1L, false)).rows(),
                service.people(new GenerateRequest(20, null, 2L, false)).rows());
    }

    @Test
    void randomSeedIsReportedAndReplays() {
        Generated<Person> first = service.people(new GenerateRequest(30, null, null, false));
        Generated<Person> replay = service.people(new GenerateRequest(30, null, first.seed(), false));
        assertEquals(first.rows(), replay.rows());

        Generated<Name> names = service.names(new GenerateRequest(30, null, null, false));
        assertEquals(names.rows(), service.names(new GenerateRequest(30, null, names.seed(), false)).rows());
    }

    @Test
    void countIsHonored() {
        assertEquals(1, service.names(new GenerateRequest(1, null, 7L, false)).rows().size());
        assertEquals(77, service.companies(new GenerateRequest(77, null, 7L, false)).rows().size());
        assertEquals(1000, service.people(new GenerateRequest(1000, null, 7L, false)).rows().size());
    }

    @Test
    void countAboveTheMaximumIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.people(new GenerateRequest(1001, null, 7L, false)));
        assertThrows(IllegalArgumentException.class, () -> new GenerateRequest(0, null, 7L, false));
    }

    @Test
    void regionIsHonoredForPeople() {
        List<Person> people = service.people(new GenerateRequest(200, Region.JPN, 3L, false)).rows();
        assertTrue(people.stream().allMatch(p -> p.region() == Region.JPN));
    }

    @Test
    void mixedRegionDrawsFromSeveral() {
        long regions = service.people(new GenerateRequest(500, null, 3L, false)).rows().stream()
                .map(Person::region).distinct().count();
        assertTrue(regions > 1, "mixed gave only one region");
    }

    @Test
    void regionChangesNames() {
        assertNotEquals(
                service.names(new GenerateRequest(50, Region.JPN, 9L, false)).rows(),
                service.names(new GenerateRequest(50, Region.BRA, 9L, false)).rows());
    }

    @Test
    void companiesAreFauxUnlessRealIsAsked() {
        List<Company> faux = service.companies(new GenerateRequest(100, null, 5L, false)).rows();
        assertTrue(faux.stream().allMatch(c -> c.domain().endsWith(".example") && c.name().endsWith("[faux]")));

        List<Person> fauxPeople = service.people(new GenerateRequest(100, null, 5L, false)).rows();
        assertTrue(fauxPeople.stream().allMatch(p -> p.employer().domain().endsWith(".example")));

        List<Company> real = service.companies(new GenerateRequest(100, null, 5L, true)).rows();
        assertTrue(real.stream().noneMatch(c -> c.domain().endsWith(".example")));
    }
}

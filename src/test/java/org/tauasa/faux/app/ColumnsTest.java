package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.tauasa.faux.model.Company;
import org.tauasa.faux.model.Name;
import org.tauasa.faux.model.Person;

class ColumnsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private final FauxService service = new FauxService(new FauxFxProperties(100, 1000));
    private final GenerateRequest request = new GenerateRequest(300, null, 11L, false);

    /** Each column's expected order, written from the record's own fields, not from Column. */
    private static Map<String, Comparator<Person>> personOrders() {
        Map<String, Comparator<Person>> m = new LinkedHashMap<>();
        m.put("First name", text(Person::firstName));
        m.put("Last name", text(Person::lastName));
        m.put("Native name", text(p -> Columns.nativeName(p.name())));
        m.put("Gender", Comparator.comparing(p -> p.gender().name()));
        m.put("Region", Comparator.comparing(p -> p.region().name()));
        m.put("Date of birth", Comparator.comparing(Person::dateOfBirth));
        m.put("Age", Comparator.comparingInt(p -> p.ageOn(TODAY)));
        m.put("Email", text(Person::email));
        m.put("Phone", text(Person::phone));
        m.put("Street", text(p -> p.address().street()));
        m.put("City", text(p -> p.address().city()));
        m.put("State", text(p -> p.address().state()));
        m.put("Postal code", text(p -> p.address().postalCode()));
        m.put("Country", text(p -> p.address().country()));
        m.put("Employer", text(p -> p.employer().name()));
        m.put("Employer city", text(p -> p.employer().city()));
        m.put("Employer state", text(p -> p.employer().state()));
        m.put("Employer country", text(p -> p.employer().country()));
        m.put("Employer domain", text(p -> p.employer().domain()));
        return m;
    }

    private static Map<String, Comparator<Company>> companyOrders() {
        Map<String, Comparator<Company>> m = new LinkedHashMap<>();
        m.put("Name", text(Company::name));
        m.put("City", text(Company::city));
        m.put("State", text(Company::state));
        m.put("Country", text(Company::country));
        m.put("Domain", text(Company::domain));
        return m;
    }

    private static Map<String, Comparator<Name>> nameOrders() {
        Map<String, Comparator<Name>> m = new LinkedHashMap<>();
        m.put("First name", text(Name::firstName));
        m.put("Last name", text(Name::lastName));
        m.put("Native name", text(Columns::nativeName));
        return m;
    }

    @Test
    void everyNameColumnSortsInItsFieldsOrderBothWays() {
        assertSortsBothWays(Columns.names(), nameOrders(), service.names(request).rows());
    }

    @Test
    void everyCompanyColumnSortsInItsFieldsOrderBothWays() {
        assertSortsBothWays(Columns.companies(), companyOrders(), service.companies(request).rows());
    }

    @Test
    void everyPersonColumnSortsInItsFieldsOrderBothWays() {
        assertSortsBothWays(Columns.people(TODAY), personOrders(), service.people(request).rows());
    }

    @Test
    void namesAndPeopleSortFirstByLastNameCompaniesByName() {
        assertEquals("Last name", Columns.defaultSort(Kind.NAMES));
        assertEquals("Last name", Columns.defaultSort(Kind.PEOPLE));
        assertEquals("Name", Columns.defaultSort(Kind.COMPANIES));

        // The default column really holds the last name, not just a title.
        Column<Person> lastName = column(Columns.people(TODAY), Columns.defaultSort(Kind.PEOPLE));
        service.people(request).rows().forEach(p -> assertEquals(p.lastName(), lastName.valueOf(p)));
        Column<Name> nameLast = column(Columns.names(), Columns.defaultSort(Kind.NAMES));
        service.names(request).rows().forEach(n -> assertEquals(n.lastName(), nameLast.valueOf(n)));
        Column<Company> companyName = column(Columns.companies(), Columns.defaultSort(Kind.COMPANIES));
        service.companies(request).rows().forEach(c -> assertEquals(c.name(), companyName.valueOf(c)));
    }

    /**
     * Every field of Person, Name, Address and Company, read from the records
     * themselves, so a field faux-person adds later fails here until it has a column.
     */
    @Test
    void everyPersonFieldHasItsOwnColumn() throws Exception {
        Map<String, String> titleOf = Map.ofEntries(
                Map.entry("name.firstName", "First name"),
                Map.entry("name.lastName", "Last name"),
                Map.entry("name.nativeName", "Native name"),
                Map.entry("gender", "Gender"),
                Map.entry("region", "Region"),
                Map.entry("dateOfBirth", "Date of birth"),
                Map.entry("email", "Email"),
                Map.entry("phone", "Phone"),
                Map.entry("address.street", "Street"),
                Map.entry("address.city", "City"),
                Map.entry("address.state", "State"),
                Map.entry("address.postalCode", "Postal code"),
                Map.entry("address.country", "Country"),
                Map.entry("employer.name", "Employer"),
                Map.entry("employer.city", "Employer city"),
                Map.entry("employer.state", "Employer state"),
                Map.entry("employer.country", "Employer country"),
                Map.entry("employer.domain", "Employer domain"));
        Person p = service.people(request).rows().get(0);
        Map<String, Object> fields = new LinkedHashMap<>();
        flatten("", p, fields);
        assertEquals(titleOf.keySet(), fields.keySet(), "a Person field has no column mapping");

        List<Column<Person>> columns = Columns.people(TODAY);
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            Object shown = column(columns, titleOf.get(field.getKey())).valueOf(p);
            Object expected = field.getValue();
            if (expected instanceof Enum<?> e) {
                expected = e.name();
            } else if (expected instanceof java.util.Optional<?> o) {
                // The native name shows as its full form, or blank when the region has none.
                expected = o.map(n -> ((org.tauasa.faux.model.NativeName) n).fullName()).orElse("");
            }
            assertEquals(expected, shown, field.getKey());
        }
    }

    @Test
    void everyCompanyFieldHasItsOwnColumn() throws Exception {
        Map<String, String> titleOf = Map.of("name", "Name", "city", "City", "state", "State", "country", "Country",
                "domain", "Domain");
        Company c = service.companies(request).rows().get(0);
        Map<String, Object> fields = new LinkedHashMap<>();
        flatten("", c, fields);
        assertEquals(titleOf.keySet(), fields.keySet(), "a Company field has no column mapping");
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            assertEquals(field.getValue(), column(Columns.companies(), titleOf.get(field.getKey())).valueOf(c));
        }
    }

    @Test
    void datesAndAgesAreValuesNotText() {
        Person p = service.people(request).rows().get(0);
        assertInstanceOf(Integer.class, column(Columns.people(TODAY), "Age").valueOf(p));
        assertInstanceOf(LocalDate.class, column(Columns.people(TODAY), "Date of birth").valueOf(p));
    }

    @Test
    void ageIsWorkedOutOnTheDateGiven() {
        Person p = service.people(request).rows().get(0);
        assertEquals(p.ageOn(TODAY), column(Columns.people(TODAY), "Age").valueOf(p));
        assertEquals(p.ageOn(TODAY.plusYears(10)), column(Columns.people(TODAY.plusYears(10)), "Age").valueOf(p));
    }

    @Test
    void textSortsIgnoringCaseThenExactly() {
        assertTrue(Column.VALUE_ORDER.compare("alice", "Bob") < 0);
        assertTrue(Column.VALUE_ORDER.compare("Bob", "alice") > 0);
        assertTrue(Column.VALUE_ORDER.compare("Ng", "ng") != 0, "case-only differences need a fixed order");
        assertEquals(0, Column.VALUE_ORDER.compare("Ng", "Ng"));
    }

    private static <T> void assertSortsBothWays(List<Column<T>> columns, Map<String, Comparator<T>> orders,
            List<T> rows) {
        assertEquals(orders.keySet(), new java.util.LinkedHashSet<>(columns.stream().map(Column::title).toList()),
                "columns and expected orders differ");
        for (Column<T> column : columns) {
            Comparator<T> expected = orders.get(column.title());
            assertNotNull(expected, column.title());

            List<T> ascending = new ArrayList<>(rows);
            ascending.sort(column.comparator());
            assertOrdered(column.title(), ascending, expected);

            List<T> descending = new ArrayList<>(rows);
            descending.sort(column.comparator().reversed());
            assertOrdered(column.title(), descending, expected.reversed());
        }
    }

    private static <T> void assertOrdered(String title, List<T> sorted, Comparator<T> expected) {
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue(expected.compare(sorted.get(i - 1), sorted.get(i)) <= 0,
                    title + " out of order at " + i + ": " + sorted.get(i - 1) + " then " + sorted.get(i));
        }
    }

    private static void flatten(String prefix, Record record, Map<String, Object> out) throws Exception {
        for (RecordComponent rc : record.getClass().getRecordComponents()) {
            Object value = rc.getAccessor().invoke(record);
            String path = prefix + rc.getName();
            if (value instanceof Record nested) {
                flatten(path + ".", nested, out);
            } else {
                out.put(path, value);
            }
        }
    }

    private static <T> Comparator<T> text(Function<T, String> field) {
        return Comparator.comparing(field, String.CASE_INSENSITIVE_ORDER);
    }

    private static <T> Column<T> column(List<Column<T>> columns, String title) {
        return columns.stream().filter(c -> c.title().equals(title)).findFirst().orElseThrow();
    }
}

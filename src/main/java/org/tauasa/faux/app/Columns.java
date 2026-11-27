package org.tauasa.faux.app;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.tauasa.faux.model.Company;
import org.tauasa.faux.model.Name;
import org.tauasa.faux.model.NativeName;
import org.tauasa.faux.model.Person;

/** The columns each kind of row shows, and the one each sorts by first. */
public final class Columns {

    public static final String LAST_NAME = "Last name";
    public static final String NATIVE_NAME = "Native name";
    public static final String COMPANY_NAME = "Name";

    private Columns() {
    }

    /** First and last name, and the name in its own script. */
    public static List<Column<Name>> names() {
        return List.of(
                new Column<>("First name", Name::firstName),
                new Column<>(LAST_NAME, Name::lastName),
                new Column<>(NATIVE_NAME, Columns::nativeName));
    }

    /**
     * The whole name in its own script as that language writes it, such as
     * 전영민 for a South Korean, or "" when faux-person has none for its region.
     */
    static String nativeName(Name n) {
        return n.nativeName().map(NativeName::fullName).orElse("");
    }

    /** Every field of a company. */
    public static List<Column<Company>> companies() {
        return List.of(
                new Column<>(COMPANY_NAME, Company::name),
                new Column<>("City", Company::city),
                new Column<>("State", Company::state),
                new Column<>("Country", Company::country),
                new Column<>("Domain", Company::domain));
    }

    /**
     * Every field of a person, with the address and employer spread into
     * columns of their own, and the person's age.
     *
     * @param today the date ages are worked out on
     */
    public static List<Column<Person>> people(LocalDate today) {
        Objects.requireNonNull(today, "today");
        return List.of(
                new Column<>("First name", Person::firstName),
                new Column<>(LAST_NAME, Person::lastName),
                new Column<>(NATIVE_NAME, p -> nativeName(p.name())),
                // Enums show and sort by name, A to Z, not in declaration order.
                new Column<>("Gender", p -> p.gender().name()),
                new Column<>("Region", p -> p.region().name()),
                new Column<>("Date of birth", Person::dateOfBirth),
                new Column<>("Age", p -> p.ageOn(today)),
                new Column<>("Email", Person::email),
                new Column<>("Phone", Person::phone),
                new Column<>("Street", p -> p.address().street()),
                new Column<>("City", p -> p.address().city()),
                new Column<>("State", p -> p.address().state()),
                new Column<>("Postal code", p -> p.address().postalCode()),
                new Column<>("Country", p -> p.address().country()),
                new Column<>("Employer", p -> p.employer().name()),
                new Column<>("Employer city", p -> p.employer().city()),
                new Column<>("Employer state", p -> p.employer().state()),
                new Column<>("Employer country", p -> p.employer().country()),
                new Column<>("Employer domain", p -> p.employer().domain()));
    }

    /** The title of the column a kind's table opens sorted by. */
    public static String defaultSort(Kind kind) {
        return switch (kind) {
            case NAMES, PEOPLE -> LAST_NAME;
            case COMPANIES -> COMPANY_NAME;
        };
    }
}

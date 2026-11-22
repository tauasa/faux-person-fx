package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tauasa.faux.model.Company;
import org.tauasa.faux.model.Person;

/**
 * The real JavaFX table, on the FX thread: it opens sorted by the default
 * column, and re-sorts when the sort column changes. Needs a display (CI runs
 * it under xvfb).
 */
class ResultsTableTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private final FauxService service = new FauxService(new FauxFxProperties(100, 1000));

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        Fx.start();
    }

    @Test
    void peopleOpenSortedByLastName() throws Exception {
        List<Person> people = service.people(new GenerateRequest(200, null, 21L, false)).rows();
        List<Person> shown = Fx.call(() -> {
            ResultsTable<Person> table = new ResultsTable<>(Columns.people(TODAY), Columns.defaultSort(Kind.PEOPLE));
            table.setRows(people);
            return new ArrayList<>(table.view().getItems());
        });

        List<Person> expected = new ArrayList<>(people);
        expected.sort(column(Columns.people(TODAY), "Last name").comparator());
        assertEquals(expected, shown);
    }

    @Test
    void companiesOpenSortedByName() throws Exception {
        List<Company> companies = service.companies(new GenerateRequest(200, null, 21L, false)).rows();
        List<Company> shown = Fx.call(() -> {
            ResultsTable<Company> table = new ResultsTable<>(Columns.companies(), Columns.defaultSort(Kind.COMPANIES));
            table.setRows(companies);
            return new ArrayList<>(table.view().getItems());
        });

        List<Company> expected = new ArrayList<>(companies);
        expected.sort(column(Columns.companies(), "Name").comparator());
        assertEquals(expected, shown);
    }

    @Test
    void choosingAnotherColumnResorts() throws Exception {
        List<Person> people = service.people(new GenerateRequest(200, null, 22L, false)).rows();
        List<Column<Person>> columns = Columns.people(TODAY);
        List<Person> byAgeDescending = Fx.call(() -> {
            ResultsTable<Person> table = new ResultsTable<>(columns, Columns.defaultSort(Kind.PEOPLE));
            table.setRows(people);
            TableView<Person> view = table.view();
            TableColumn<Person, ?> age = view.getColumns().stream()
                    .filter(c -> c.getText().equals("Age")).findFirst().orElseThrow();
            age.setSortType(TableColumn.SortType.DESCENDING);
            view.getSortOrder().setAll(List.of(age));
            return new ArrayList<>(view.getItems());
        });

        Column<Person> age = column(columns, "Age");
        for (int i = 1; i < byAgeDescending.size(); i++) {
            int before = (Integer) age.valueOf(byAgeDescending.get(i - 1));
            int after = (Integer) age.valueOf(byAgeDescending.get(i));
            assertTrue(before >= after, "ages out of order at " + i + ": " + before + " then " + after);
        }
        assertEquals(people.size(), byAgeDescending.size());
    }

    @Test
    void clearEmptiesTheTable() throws Exception {
        List<Person> people = service.people(new GenerateRequest(10, null, 23L, false)).rows();
        int size = Fx.call(() -> {
            ResultsTable<Person> table = new ResultsTable<>(Columns.people(TODAY), Columns.defaultSort(Kind.PEOPLE));
            table.setRows(people);
            table.clear();
            return table.view().getItems().size();
        });
        assertEquals(0, size);
    }

    @Test
    void copiesSelectedRowsAsTabSeparatedTextInShownOrder() throws Exception {
        List<org.tauasa.faux.model.Name> names = List.of(
                new org.tauasa.faux.model.Name("Ana", "Zed"),
                new org.tauasa.faux.model.Name("Bo\tb", "Avery"),
                new org.tauasa.faux.model.Name("Cy", "Moss"));
        String[] copied = Fx.call(() -> {
            ResultsTable<org.tauasa.faux.model.Name> table =
                    new ResultsTable<>(Columns.names(), Columns.defaultSort(Kind.NAMES));
            table.setRows(names);
            String none = table.selectedAsTsv();
            table.view().getSelectionModel().select(2); // Zed, last by last name
            table.view().getSelectionModel().select(0); // Avery, first
            return new String[] {none, table.selectedAsTsv()};
        });
        assertEquals("", copied[0]);
        assertEquals("First name\tLast name\nBo b\tAvery\nAna\tZed\n", copied[1]);
    }

    private static <T> Column<T> column(List<Column<T>> columns, String title) {
        return columns.stream().filter(c -> c.title().equals(title)).findFirst().orElseThrow();
    }
}

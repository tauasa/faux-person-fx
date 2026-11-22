package org.tauasa.faux.app;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

/**
 * A {@link TableView} built from {@link Column}s. Every column sorts, and the
 * table opens sorted ascending by the column named.
 *
 * @param <T> the row type
 */
final class ResultsTable<T> {

    private final ObservableList<T> rows = FXCollections.observableArrayList();
    private final TableView<T> view = new TableView<>();
    private final List<Column<T>> columns;

    ResultsTable(List<Column<T>> columns, String sortFirstBy) {
        this.columns = List.copyOf(columns);
        view.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableColumn<T, Object> sortColumn = null;
        for (Column<T> column : columns) {
            TableColumn<T, Object> tc = new TableColumn<>(column.title());
            tc.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(column.valueOf(cell.getValue())));
            tc.setComparator(Column.VALUE_ORDER);
            tc.setSortable(true);
            view.getColumns().add(tc);
            if (column.title().equals(sortFirstBy)) {
                sortColumn = tc;
            }
        }
        if (sortColumn == null) {
            throw new IllegalArgumentException("no column titled " + sortFirstBy);
        }

        // The table sorts this view of the rows, never the rows themselves.
        SortedList<T> sorted = new SortedList<>(rows);
        sorted.comparatorProperty().bind(view.comparatorProperty());
        view.setItems(sorted);

        sortColumn.setSortType(TableColumn.SortType.ASCENDING);
        view.getSortOrder().add(sortColumn);
        view.setPlaceholder(new Label("Nothing yet. Press Generate."));
    }

    /**
     * What a double-click on a row does, given the row. Double-clicking an
     * empty line below the rows, or with another button, does nothing.
     */
    void setOnRowOpen(Consumer<T> open) {
        view.setRowFactory(table -> {
            TableRow<T> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !row.isEmpty()) {
                    open.accept(row.getItem());
                }
            });
            return row;
        });
    }

    List<Column<T>> columns() {
        return columns;
    }

    TableView<T> view() {
        return view;
    }

    void setRows(List<T> newRows) {
        rows.setAll(newRows);
    }

    void clear() {
        rows.clear();
    }

    int size() {
        return rows.size();
    }

    /** Every row, in the order the table shows them, written as {@code format}. */
    void export(ExportFormat format, Writer out) throws IOException {
        Exporter.write(columns, List.copyOf(view.getItems()), format, out);
    }

    void selectAll() {
        view.getSelectionModel().selectAll();
    }

    boolean hasSelection() {
        return !view.getSelectionModel().isEmpty();
    }

    /**
     * The selected rows, in the order the table shows them, as tab-separated
     * text with a header line: what a spreadsheet pastes as columns. Empty
     * when nothing is selected.
     */
    String selectedAsTsv() {
        List<Integer> indices = new ArrayList<>(view.getSelectionModel().getSelectedIndices());
        if (indices.isEmpty()) {
            return "";
        }
        indices.sort(null);
        StringBuilder out = new StringBuilder();
        out.append(columns.stream().map(c -> cell(c.title())).collect(Collectors.joining("\t"))).append('\n');
        for (int i : indices) {
            T row = view.getItems().get(i);
            out.append(columns.stream().map(c -> cell(String.valueOf(c.valueOf(row))))
                    .collect(Collectors.joining("\t"))).append('\n');
        }
        return out.toString();
    }

    /** A tab or line break inside a value would split it into two cells. */
    private static String cell(String text) {
        return text.replaceAll("[\\t\\r\\n]", " ");
    }
}

package org.tauasa.faux.app;

import java.util.List;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/**
 * One row in a dialog of its own: every column's title and value, top to
 * bottom. Values are read-only text boxes, so each can be selected and copied.
 */
final class RecordDialog {

    private RecordDialog() {
    }

    /**
     * Builds the dialog without showing it.
     *
     * @param owner   the window it belongs to and blocks
     * @param kind    what the row is, for the title
     * @param heading the line above the fields, such as the person's name
     * @param columns the fields to show, in order
     * @param row     the row
     */
    static <T> Dialog<Void> create(Window owner, Kind kind, String heading, List<Column<T>> columns, T row) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(kind.singularTitle() + ": " + heading);
        dialog.setResizable(true);

        Label title = new Label(heading);
        title.setId("recordHeading");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(6);
        ColumnConstraints labels = new ColumnConstraints();
        labels.setHalignment(HPos.RIGHT);
        ColumnConstraints values = new ColumnConstraints();
        values.setHgrow(Priority.ALWAYS);
        values.setMinWidth(280);
        fields.getColumnConstraints().addAll(labels, values);

        for (int i = 0; i < columns.size(); i++) {
            Column<T> column = columns.get(i);
            Label label = new Label(column.title());
            TextField value = new TextField(String.valueOf(column.valueOf(row)));
            value.setId("record-" + Exporter.jsonKey(column.title()));
            value.setEditable(false);
            value.setFocusTraversable(true);
            label.setLabelFor(value);
            fields.addRow(i, label, value);
        }

        ScrollPane scroll = new ScrollPane(fields);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(12, title, scroll);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        return dialog;
    }
}

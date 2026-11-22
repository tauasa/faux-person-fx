package org.tauasa.faux.app;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

/** Help > About: the project, its license, the licenses it carries, and a link to its repo. */
final class AboutDialog {

    private AboutDialog() {
    }

    /**
     * Builds the dialog without showing it.
     *
     * @param owner    the window it belongs to and blocks
     * @param about    the versions to show
     * @param openLink opens a URL in the browser
     */
    static Dialog<Void> create(Window owner, About about, Consumer<String> openLink) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("About " + About.NAME);

        Label name = new Label(About.NAME);
        name.setId("aboutName");
        name.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label version = new Label("Version " + about.version());
        version.setId("aboutVersion");

        Label description = wrapped("Generates random names, companies and people with faux-person, "
                + "and shows them in tables you can sort by any column.");

        Hyperlink link = new Hyperlink(About.PROJECT_URL);
        link.setId("aboutLink");
        link.setOnAction(e -> openLink.accept(About.PROJECT_URL));

        Label license = wrapped("MIT License. " + About.COPYRIGHT + ".");
        license.setId("aboutLicense");
        license.setStyle("-fx-font-weight: bold;");

        Label builtWith = wrapped("Built with faux-person " + about.fauxPersonVersion() + " (MIT License); "
                + "JavaFX (GPL v2 with the Classpath Exception); Spring Boot, Spring Framework, Micrometer, "
                + "Log4j API, SnakeYAML, Commons Logging and JSpecify (Apache License 2.0); "
                + "Logback (EPL 1.0 or LGPL 2.1); SLF4J (MIT License); and Jakarta Annotations "
                + "(EPL 2.0 or GPL v2 with the Classpath Exception).");
        builtWith.setId("aboutBuiltWith");

        Label data = wrapped("Name, company and place data comes from faux-person, which built it from "
                + "Wikidata (CC0 1.0); GeoNames (geonames.org), under the Creative Commons Attribution 4.0 "
                + "License (creativecommons.org/licenses/by/4.0), with places sampled per state, joined to "
                + "postal codes and reduced to name, state and one ZIP code; and US Census Bureau TIGER/Line "
                + "Shapefiles 2024, a work of the US Government not subject to copyright in the United States.");
        data.setId("aboutData");

        VBox content = new VBox(8, name, version, description, link, license, builtWith, data);
        content.setPadding(new Insets(16));
        content.setPrefWidth(480);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        return dialog;
    }

    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}

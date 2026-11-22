package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Help > About: modal, names the project and its license, and its link opens the repo. Needs a display. */
class AboutDialogTest {

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        Fx.start();
    }

    @Test
    void showsProjectLicenseAndLinksToTheRepo() throws Exception {
        List<String> opened = new CopyOnWriteArrayList<>();
        Stage owner = Fx.call(() -> {
            Stage s = new Stage();
            s.setScene(new javafx.scene.Scene(new javafx.scene.layout.Pane(), 200, 100));
            return s;
        });
        Dialog<Void> dialog = Fx.call(() -> AboutDialog.create(owner, new About("1.2.3", "0.2.0"), opened::add));

        assertEquals(Modality.APPLICATION_MODAL, dialog.getModality());
        assertSame(owner, dialog.getOwner());
        assertEquals("About faux-person-fx", dialog.getTitle());

        String version = Fx.call(() -> ((Label) dialog.getDialogPane().lookup("#aboutVersion")).getText());
        assertEquals("Version 1.2.3", version);
        String license = Fx.call(() -> ((Label) dialog.getDialogPane().lookup("#aboutLicense")).getText());
        assertTrue(license.startsWith("MIT License."), license);
        assertTrue(license.contains("Tauasa Timoteo"), license);
        String data = Fx.call(() -> ((Label) dialog.getDialogPane().lookup("#aboutData")).getText());
        assertTrue(data.contains("GeoNames") && data.contains("Creative Commons Attribution 4.0"), "GeoNames needs attribution: " + data);
        assertTrue(data.contains("sampled per state"), "CC BY asks that changes be noted: " + data);

        String builtWith = Fx.call(() -> ((Label) dialog.getDialogPane().lookup("#aboutBuiltWith")).getText());
        assertTrue(builtWith.contains("faux-person 0.2.0 (MIT License)"), builtWith);

        Hyperlink link = Fx.call(() -> (Hyperlink) dialog.getDialogPane().lookup("#aboutLink"));
        assertEquals("https://github.com/tauasa/faux-person-fx", Fx.call(link::getText));
        Fx.run(link::fire);
        assertEquals(List.of("https://github.com/tauasa/faux-person-fx"), opened);
    }

    @Test
    void anUnfilteredVersionShowsAsDev() {
        About about = new About("@project.version@", "@faux-person.version@");
        assertEquals("dev", about.version());
        assertEquals("unknown", about.fauxPersonVersion());
    }
}

package org.tauasa.faux.app;

import javafx.application.Preloader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * The splash screen: a small undecorated window with a progress bar, shown
 * before Spring starts and closed once the main window has painted.
 * {@link Launcher} names it in the {@code javafx.preloader} property.
 */
public class SplashPreloader extends Preloader {

    private Stage stage;
    private ProgressBar bar;
    private Label message;

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        Label name = new Label(About.NAME);
        name.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label tagline = new Label("Random names, companies and people");

        bar = new ProgressBar(0);
        bar.setId("splashProgress");
        bar.setPrefWidth(340);

        message = new Label("Starting…");
        message.setId("splashMessage");
        message.setStyle("-fx-text-fill: derive(-fx-text-base-color, 35%);");

        VBox root = new VBox(10, name, tagline, bar, message);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setStyle("-fx-border-color: derive(-fx-base, -25%); -fx-border-width: 1;");

        Scene scene = new Scene(root);
        // Match the appearance the app will open in, so it doesn't flash light then dark.
        Appearance appearance = AppearanceFile.forSplash();
        boolean dark = appearance == Appearance.SYSTEM ? new SystemTheme().isDark() : appearance == Appearance.DARK;
        if (dark) {
            scene.getStylesheets().add(Appearance.DARK_STYLESHEET);
        }

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(About.NAME);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void handleApplicationNotification(PreloaderNotification info) {
        if (info instanceof Splash.Progress progress) {
            // Never move backwards: steps can arrive out of order on a fast start.
            bar.setProgress(Math.max(bar.getProgress(), progress.fraction()));
            message.setText(progress.message());
        } else if (info instanceof Splash.Ready) {
            bar.setProgress(1);
            stage.hide();
        }
    }

    /** Stays up through BEFORE_START, the default's cue to hide, until the window has painted. */
    @Override
    public void handleStateChangeNotification(StateChangeNotification info) {
    }

    /** A failed start closes the splash, so the error isn't hidden behind it. */
    @Override
    public boolean handleErrorNotification(ErrorNotification info) {
        if (stage != null) {
            stage.hide();
        }
        return false;
    }

    /** For tests: the stage the splash is in. */
    Stage stage() {
        return stage;
    }
}

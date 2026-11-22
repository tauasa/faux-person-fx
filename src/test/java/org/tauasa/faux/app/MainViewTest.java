package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives the real window: generate, tabs, clear, and bad input. Needs a display. */
class MainViewTest {

    private Stage stage;
    private Scene scene;
    private MainView view;
    /** The OS's light or dark setting, as the app's System mode sees it. */
    private final java.util.concurrent.atomic.AtomicBoolean osDark = new java.util.concurrent.atomic.AtomicBoolean();
    private final SystemTheme systemTheme = new SystemTheme(
            cmd -> osDark.get() ? java.util.Optional.of("Dark\n") : java.util.Optional.empty(), "Mac OS X");
    private final java.util.List<String> opened = new java.util.concurrent.CopyOnWriteArrayList<>();

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        Fx.start();
    }

    @BeforeEach
    void openWindow() throws Exception {
        FauxFxProperties properties = new FauxFxProperties(100, 1000);
        view = new MainView(new FauxService(properties), properties, new About("1.2.3", "0.2.0"), systemTheme);
        stage = Fx.call(() -> {
            Stage s = new Stage();
            view.show(s, opened::add);
            return s;
        });
        scene = stage.getScene();
    }

    @AfterEach
    void closeWindow() throws Exception {
        Fx.run(stage::close);
    }

    @Test
    void generateFillsTheTabAndReportsTheSeed() throws Exception {
        assertEquals("No names yet.", status());
        generate("25", "42");
        assertEquals("25 names, seed 42", status());
        assertEquals(25, rowsOnSelectedTab());
        assertFalse(Fx.call(() -> node("#clear", Button.class).isDisabled()));
    }

    @Test
    void blankSeedReportsTheOneUsed() throws Exception {
        generate("10", "");
        String status = status();
        assertTrue(status.matches("10 names, seed -?\\d+"), status);
    }

    @Test
    void otherTabsKeepTheirRows() throws Exception {
        generate("25", "42");
        selectTab(2);
        assertEquals("No people yet.", status());
        assertEquals(0, rowsOnSelectedTab());
        generate("5", "7");
        assertEquals("5 people, seed 7", status());

        selectTab(0);
        assertEquals("25 names, seed 42", status());
        assertEquals(25, rowsOnSelectedTab());
    }

    @Test
    void controlsThatDontApplyAreDisabled() throws Exception {
        selectTab(0);
        assertTrue(Fx.call(() -> node("#realCompanies", CheckBox.class).isDisabled()), "names have no company");
        assertFalse(Fx.call(() -> node("#region", ComboBox.class).isDisabled()));
        selectTab(1);
        assertTrue(Fx.call(() -> node("#region", ComboBox.class).isDisabled()), "companies have no region");
        assertFalse(Fx.call(() -> node("#realCompanies", CheckBox.class).isDisabled()));
        selectTab(2);
        assertFalse(Fx.call(() -> node("#region", ComboBox.class).isDisabled()));
        assertFalse(Fx.call(() -> node("#realCompanies", CheckBox.class).isDisabled()));
    }

    @Test
    void clearEmptiesOnlyTheSelectedTab() throws Exception {
        generate("25", "42");
        selectTab(1);
        generate("8", "3");
        Fx.run(() -> node("#clear", Button.class).fire());
        assertEquals("No companies yet.", status());
        assertEquals(0, rowsOnSelectedTab());
        selectTab(0);
        assertEquals(25, rowsOnSelectedTab());
    }

    @Test
    void badSeedIsReportedAndGeneratesNothing() throws Exception {
        generate("25", "42");
        typeAndGenerate("25", "not a number");
        assertTrue(status().startsWith("Seed must be a whole number"), status());
        assertEquals(25, rowsOnSelectedTab());
    }

    @Test
    void badCountIsReported() throws Exception {
        typeAndGenerate("0", "");
        assertEquals("Count must be from 1 to 1000", status());
        typeAndGenerate("1001", "");
        assertEquals("Count must be from 1 to 1000", status());
        assertEquals(0, rowsOnSelectedTab());
    }

    @Test
    void enterInTheCountBoxGeneratesAndJunkDoesntThrow() throws Exception {
        Fx.run(() -> {
            seedField().setText("5");
            TextField editor = node("#count", Spinner.class).getEditor();
            editor.setText("junk");
            editor.fireEvent(enter());
        });
        assertTrue(status().startsWith("Count must be a whole number"), status());

        Fx.run(() -> {
            TextField editor = node("#count", Spinner.class).getEditor();
            editor.setText("12");
            editor.fireEvent(enter());
        });
        awaitDone();
        assertEquals("12 names, seed 5", status());
    }

    @Test
    void regionsAreListedAToZAfterMixed() throws Exception {
        java.util.List<String> items = Fx.call(() -> java.util.List.copyOf(
                node("#region", ComboBox.class).getItems().stream().map(Object::toString).toList()));
        assertEquals(MainView.MIXED, items.get(0));
        java.util.List<String> rest = items.subList(1, items.size());
        assertEquals(rest.stream().sorted().toList(), rest);
        assertEquals(org.tauasa.faux.model.Region.values().length, rest.size());
    }

    @Test
    void menuBarHasTheStandardMenus() throws Exception {
        java.util.List<String> menus = Fx.call(() -> node("#menuBar", MenuBar.class).getMenus().stream()
                .map(Menu::getText).toList());
        assertEquals(java.util.List.of("File", "Edit", "View", "Help"), menus);
        assertEquals("About faux-person-fx", Fx.call(() -> menuItem("menuAbout").getText()));
    }

    @Test
    void theMenuBarIsVisibleInTheWindow() throws Exception {
        MenuBar bar = Fx.call(() -> node("#menuBar", MenuBar.class));
        assertFalse(Fx.call(bar::isUseSystemMenuBar), "menus go to the screen's menu bar, not the window");
        assertTrue(Fx.call(() -> bar.isVisible() && bar.isManaged()), "menu bar hidden");
        assertTrue(Fx.call(bar::getHeight) > 0, "menu bar takes no space in the window");
    }

    @Test
    void fileGenerateAndEditClearActOnTheSelectedTab() throws Exception {
        selectTab(2);
        Fx.run(() -> {
            node("#count", Spinner.class).getEditor().setText("15");
            seedField().setText("8");
            menuItem("menuGenerate").fire();
        });
        awaitDone();
        assertEquals("15 people, seed 8", status());
        selectTab(0);
        assertEquals("No names yet.", status(), "File > Generate filled the wrong tab");
        selectTab(2);
        Fx.run(() -> menuItem("menuClear").fire());
        assertEquals("No people yet.", status());
        assertTrue(Fx.call(() -> menuItem("menuClear").isDisable()), "nothing left to clear");
    }

    @Test
    void viewMenuAndTabsStayInStep() throws Exception {
        Fx.run(() -> menuItem("menuViewPeople").fire());
        assertEquals(2, Fx.call(() -> node("#tabs", TabPane.class).getSelectionModel().getSelectedIndex()));
        selectTab(1);
        assertTrue(Fx.call(() -> ((RadioMenuItem) menuItem("menuViewCompanies")).isSelected()));
        assertFalse(Fx.call(() -> ((RadioMenuItem) menuItem("menuViewPeople")).isSelected()));
    }

    @Test
    void selectAllSelectsEveryRowAndCopyPutsThemOnTheClipboard() throws Exception {
        Fx.run(() -> node("#tabs", TabPane.class).requestFocus());
        assertTrue(Fx.call(() -> menuItem("menuCopy").isDisable()), "nothing to copy yet");
        generate("4", "2");
        Fx.run(() -> node("#tabs", TabPane.class).requestFocus());
        Fx.run(() -> menuItem("menuSelectAll").fire());
        assertEquals(4, Fx.call(() -> selectedTable().getSelectionModel().getSelectedIndices().size()));
        assertFalse(Fx.call(() -> menuItem("menuCopy").isDisable()));

        String saved = Fx.call(() -> javafx.scene.input.Clipboard.getSystemClipboard().getString());
        try {
            Fx.run(() -> menuItem("menuCopy").fire());
            String copied = Fx.call(() -> javafx.scene.input.Clipboard.getSystemClipboard().getString());
            String[] lines = copied.split("\n");
            assertEquals("First name\tLast name", lines[0]);
            assertEquals(5, lines.length);
        } finally {
            restoreClipboard(saved);
        }
    }

    @Test
    void inATextBoxSelectAllAndCopyActOnItsText() throws Exception {
        generate("4", "2");
        Fx.run(() -> {
            TextField seed = seedField();
            seed.setText("12345");
            seed.requestFocus();
        });
        // Focus moving into the box enables Copy even with no rows selected.
        assertFalse(Fx.call(() -> menuItem("menuCopy").isDisable()), "Copy stayed disabled in the seed box");
        Fx.run(() -> menuItem("menuSelectAll").fire());
        assertEquals("12345", Fx.call(() -> seedField().getSelectedText()));
        assertEquals(0, Fx.call(() -> selectedTable().getSelectionModel().getSelectedIndices().size()),
                "Select All in a text box selected the rows");

        String saved = Fx.call(() -> javafx.scene.input.Clipboard.getSystemClipboard().getString());
        try {
            Fx.run(() -> menuItem("menuCopy").fire());
            assertEquals("12345", Fx.call(() -> javafx.scene.input.Clipboard.getSystemClipboard().getString()));
        } finally {
            restoreClipboard(saved);
        }
    }

    @Test
    void menuShortcutsAreTheStandardOnes() throws Exception {
        assertEquals("Shortcut+C", Fx.call(() -> menuItem("menuCopy").getAccelerator().getName()));
        assertEquals("Shortcut+A", Fx.call(() -> menuItem("menuSelectAll").getAccelerator().getName()));
        assertEquals("Shortcut+S", Fx.call(() -> menuItem("menuSave").getAccelerator().getName()));
        assertEquals("Shortcut+G", Fx.call(() -> menuItem("menuGenerate").getAccelerator().getName()));
        assertEquals("Shortcut+1", Fx.call(() -> menuItem("menuViewNames").getAccelerator().getName()));
    }

    @Test
    void aBlankSeedIsFilledInWithTheOneUsed() throws Exception {
        generate("10", "");
        String shown = Fx.call(() -> seedField().getText());
        assertTrue(shown.matches("-?\\d+"), "seed box shows: " + shown);
        assertEquals("10 names, seed " + shown, status());
    }

    @Test
    void eachTabShowsItsOwnLastSeed() throws Exception {
        generate("10", "11");
        selectTab(2);
        assertEquals("", Fx.call(() -> seedField().getText()), "a tab with no runs shows no seed");
        generate("5", "22");
        assertEquals("22", Fx.call(() -> seedField().getText()));
        selectTab(0);
        assertEquals("11", Fx.call(() -> seedField().getText()));
        selectTab(2);
        assertEquals("22", Fx.call(() -> seedField().getText()));

        // Clear empties the table but the box keeps the last seed, to make it again.
        Fx.run(() -> node("#clear", Button.class).fire());
        assertEquals("22", Fx.call(() -> seedField().getText()));
    }

    @Test
    void theListHoldsThisTabsSeedsMostRecentFirst() throws Exception {
        generate("3", "1");
        generate("3", "2");
        generate("3", "3");
        generate("3", "1");
        assertEquals(java.util.List.of("1", "3", "2"), Fx.call(() -> java.util.List.copyOf(seedBox().getItems())));
        selectTab(1);
        assertTrue(Fx.call(() -> seedBox().getItems().isEmpty()), "another tab's seeds leaked in");
    }

    @Test
    void clickingTheSeedBoxOpensTheList() throws Exception {
        Fx.run(() -> seedField().fireEvent(click()));
        assertFalse(Fx.call(() -> seedBox().isShowing()), "opened an empty list");
        generate("3", "1");
        Fx.run(() -> seedField().fireEvent(click()));
        assertTrue(Fx.call(() -> seedBox().isShowing()));
        Fx.run(() -> seedBox().hide());
    }

    @Test
    void choosingAPastSeedRestoresItsSettingsAndRemakesTheSameRows() throws Exception {
        selectTab(2);
        Fx.run(() -> {
            node("#region", ComboBox.class).getSelectionModel().select("JPN");
            node("#realCompanies", CheckBox.class).setSelected(true);
        });
        generate("12", "5");
        java.util.List<Object> first = Fx.call(() -> java.util.List.copyOf(selectedTable().getItems()));

        Fx.run(() -> {
            node("#region", ComboBox.class).getSelectionModel().select(MainView.MIXED);
            node("#realCompanies", CheckBox.class).setSelected(false);
        });
        generate("30", "6");

        Fx.run(() -> {
            seedBox().show();
            seedBox().setValue("5");
            seedBox().hide();
        });
        assertEquals("12", Fx.call(() -> node("#count", Spinner.class).getEditor().getText()));
        assertEquals("JPN", Fx.call(() -> node("#region", ComboBox.class).getValue()));
        assertTrue(Fx.call(() -> node("#realCompanies", CheckBox.class).isSelected()));
        assertEquals("5", Fx.call(() -> seedField().getText()));

        Fx.run(() -> node("#generate", Button.class).fire());
        awaitDone();
        assertEquals("12 people, seed 5", status());
        assertEquals(first, Fx.call(() -> java.util.List.copyOf(selectedTable().getItems())));
    }

    @Test
    void typingASeedDoesntChangeTheOtherSettings() throws Exception {
        generate("12", "5");
        Fx.run(() -> {
            node("#count", Spinner.class).getEditor().setText("40");
            seedField().setText("5");
            seedBox().setValue("5");
        });
        assertEquals("40", Fx.call(() -> node("#count", Spinner.class).getEditor().getText()));
    }

    @Test
    void randomClearsTheSeedSoTheNextRunPicksANewOne() throws Exception {
        generate("5", "77");
        Fx.run(() -> node("#randomSeed", Button.class).fire());
        assertEquals("", Fx.call(() -> seedField().getText()));
        Fx.run(() -> node("#generate", Button.class).fire());
        awaitDone();
        String shown = Fx.call(() -> seedField().getText());
        assertTrue(shown.matches("-?\\d+") && !shown.equals("77"), "seed box shows: " + shown);
    }

    @Test
    void enterInTheSeedBoxGeneratesOnce() throws Exception {
        Fx.run(() -> {
            node("#count", Spinner.class).getEditor().setText("6");
            seedField().setText("9");
            seedField().fireEvent(enter());
        });
        awaitDone();
        assertEquals("6 names, seed 9", status());
        assertEquals(java.util.List.of("9"), Fx.call(() -> java.util.List.copyOf(seedBox().getItems())));
    }

    @Test
    void theNextSessionOpensWithEachTabsHistory(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        String file = dir.resolve("history.txt").toString();
        Stage first = openWith(file);
        Scene firstScene = first.getScene();
        Fx.run(() -> {
            ((Spinner<?>) firstScene.lookup("#count")).getEditor().setText("7");
            ((ComboBox<?>) firstScene.lookup("#seed")).getEditor().setText("31");
            ((Button) firstScene.lookup("#generate")).fire();
        });
        awaitText(firstScene, "7 names, seed 31");
        Fx.run(() -> {
            ((TabPane) firstScene.lookup("#tabs")).getSelectionModel().select(2);
            ((ComboBox<?>) firstScene.lookup("#seed")).getEditor().setText("32");
            ((Button) firstScene.lookup("#generate")).fire();
        });
        awaitText(firstScene, "7 people, seed 32");
        Fx.run(first::close);

        Stage second = openWith(file);
        Scene secondScene = second.getScene();
        try {
            assertEquals("31", Fx.call(() -> ((ComboBox<?>) secondScene.lookup("#seed")).getEditor().getText()),
                    "the Names tab opened without its last seed");
            assertEquals(java.util.List.of("31"),
                    Fx.call(() -> java.util.List.copyOf(((ComboBox<?>) secondScene.lookup("#seed")).getItems())));
            Fx.run(() -> ((TabPane) secondScene.lookup("#tabs")).getSelectionModel().select(2));
            assertEquals("32", Fx.call(() -> ((ComboBox<?>) secondScene.lookup("#seed")).getEditor().getText()));
        } finally {
            Fx.run(second::close);
        }
    }

    private static Stage openWith(String historyFile) throws Exception {
        return openWith(new FauxFxProperties(100, 1000, historyFile));
    }

    private static Stage openWith(FauxFxProperties properties) throws Exception {
        MainView other = new MainView(new FauxService(properties), properties, new About("1.2.3", "0.2.0"),
                new SystemTheme(cmd -> java.util.Optional.empty(), "Mac OS X"));
        return Fx.call(() -> {
            Stage s = new Stage();
            other.show(s, url -> { });
            return s;
        });
    }

    private static void awaitText(Scene scene, String expected) throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (!expected.equals(Fx.call(() -> ((Label) scene.lookup("#status")).getText()))) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("status never became: " + expected);
            }
            Thread.sleep(20);
        }
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> seedBox() {
        return node("#seed", ComboBox.class);
    }

    private TextField seedField() {
        return seedBox().getEditor();
    }

    private static javafx.scene.input.MouseEvent click() {
        return new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null);
    }

    private TableView<?> selectedTable() {
        return (TableView<?>) node("#tabs", TabPane.class).getSelectionModel().getSelectedItem().getContent();
    }

    private static void restoreClipboard(String saved) throws Exception {
        Fx.run(() -> {
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(saved == null ? "" : saved);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });
    }

    @Test
    void saveWritesTheSelectedTabInTheOrderShown(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        assertTrue(Fx.call(() -> menuItem("menuSave").isDisable()), "nothing to save yet");
        generate("30", "6");
        assertFalse(Fx.call(() -> menuItem("menuSave").isDisable()));

        java.nio.file.Path written = Fx.call(() -> view.save(dir.resolve("names"), ExportFormat.CSV));
        assertEquals(dir.resolve("names.csv"), written);
        assertTrue(status().startsWith("Saved 30 names to names.csv"), status());

        java.util.List<String> lines = java.nio.file.Files.readAllLines(written);
        assertEquals("First name,Last name", lines.get(0));
        assertEquals(31, lines.size());
        java.util.List<String> lastNames = lines.subList(1, lines.size()).stream()
                .map(l -> l.substring(l.indexOf(',') + 1)).toList();
        assertEquals(lastNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), lastNames,
                "saved in the table's last-name order");

        java.nio.file.Path json = Fx.call(() -> view.save(dir.resolve("names.json"), ExportFormat.CSV));
        java.util.List<?> rows = com.jayway.jsonpath.JsonPath.read(json.toFile(), "$");
        assertEquals(30, rows.size());
    }

    @Test
    void aSaveThatFailsSaysSo(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        generate("3", "6");
        java.nio.file.Path missing = dir.resolve("no-such-folder").resolve("names.csv");
        assertEquals(null, Fx.call(() -> view.save(missing, ExportFormat.CSV)));
        assertTrue(status().startsWith("Couldn't save names.csv"), status());
    }

    private MenuItem menuItem(String id) {
        return node("#menuBar", MenuBar.class).getMenus().stream()
                .flatMap(m -> items(m).stream())
                .filter(i -> id.equals(i.getId())).findFirst().orElseThrow(() -> new AssertionError("no " + id));
    }

    /** A menu's items, and its submenus' items, all the way down. */
    private static java.util.List<MenuItem> items(Menu menu) {
        java.util.List<MenuItem> all = new java.util.ArrayList<>();
        for (MenuItem item : menu.getItems()) {
            all.add(item);
            if (item instanceof Menu sub) {
                all.addAll(items(sub));
            }
        }
        return all;
    }

    @Test
    void helpHasAppearanceAboveAbout() throws Exception {
        java.util.List<String> help = Fx.call(() -> node("#menuBar", MenuBar.class).getMenus().get(3).getItems()
                .stream().map(i -> i.getText() == null ? "---" : i.getText()).toList());
        assertEquals(java.util.List.of("Appearance", "---", "About faux-person-fx"), help);
        java.util.List<String> choices = Fx.call(() -> ((Menu) menuItem("menuAppearance")).getItems().stream()
                .map(MenuItem::getText).toList());
        assertEquals(java.util.List.of("Light", "Dark", "System"), choices);
        assertTrue(Fx.call(() -> ((RadioMenuItem) menuItem("menuAppearanceSystem")).isSelected()),
                "System should be the starting choice");
    }

    @Test
    void lightAndDarkSwitchTheWholeWindow() throws Exception {
        assertFalse(Fx.call(view::showsDark), "opened dark with the OS light");
        Fx.run(() -> menuItem("menuAppearanceDark").fire());
        assertTrue(Fx.call(view::showsDark));
        assertTrue(Fx.call(() -> ((RadioMenuItem) menuItem("menuAppearanceDark")).isSelected()));
        Fx.run(() -> menuItem("menuAppearanceLight").fire());
        assertFalse(Fx.call(view::showsDark));
        assertFalse(Fx.call(() -> ((RadioMenuItem) menuItem("menuAppearanceDark")).isSelected()));
    }

    @Test
    void systemFollowsTheOs() throws Exception {
        Fx.run(() -> menuItem("menuAppearanceLight").fire());
        osDark.set(true);
        Fx.run(() -> menuItem("menuAppearanceSystem").fire());
        awaitDark(true);

        // Choosing System again asks the OS again, rather than reusing the last answer.
        osDark.set(false);
        Fx.run(() -> menuItem("menuAppearanceSystem").fire());
        awaitDark(false);

        // Light and Dark ignore the OS.
        osDark.set(true);
        Fx.run(() -> menuItem("menuAppearanceLight").fire());
        Thread.sleep(200);
        assertFalse(Fx.call(view::showsDark), "Light followed the OS");
    }

    @Test
    void theAboutDialogFollowsTheAppearance() throws Exception {
        Fx.run(() -> menuItem("menuAppearanceDark").fire());
        boolean dark = Fx.call(() -> AboutDialog.create(stage, new About("1", "2"), url -> { })
                .getDialogPane().getScene().getStylesheets().contains(Appearance.DARK_STYLESHEET));
        assertTrue(dark, "About stayed light in a dark window");
    }

    @Test
    void theAppearanceIsKeptForTheNextSession(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        FauxFxProperties properties = new FauxFxProperties(100, 1000, "", dir.resolve("appearance.txt").toString());
        Stage first = openWith(properties);
        Fx.run(() -> ((RadioMenuItem) appearanceItem(first, "Dark")).fire());
        Fx.run(first::close);

        Stage second = openWith(properties);
        try {
            assertTrue(Fx.call(() -> second.getScene().getStylesheets().contains(Appearance.DARK_STYLESHEET)),
                    "the next session opened light");
            assertTrue(Fx.call(() -> ((RadioMenuItem) appearanceItem(second, "Dark")).isSelected()));
        } finally {
            Fx.run(second::close);
        }
    }

    private static MenuItem appearanceItem(Stage window, String label) {
        MenuBar bar = (MenuBar) window.getScene().lookup("#menuBar");
        return items(bar.getMenus().get(3)).stream()
                .filter(i -> ("menuAppearance" + label).equals(i.getId())).findFirst().orElseThrow();
    }

    private void awaitDark(boolean expected) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (Fx.call(view::showsDark) != expected) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("never became " + (expected ? "dark" : "light"));
            }
            Thread.sleep(20);
        }
    }

    private void generate(String count, String seed) throws Exception {
        typeAndGenerate(count, seed);
        awaitDone();
    }

    private void typeAndGenerate(String count, String seed) throws Exception {
        Fx.run(() -> {
            node("#count", Spinner.class).getEditor().setText(count);
            seedField().setText(seed);
            node("#generate", Button.class).fire();
        });
    }

    private void awaitDone() throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (status().startsWith("Generating")) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("still generating");
            }
            Thread.sleep(20);
        }
    }

    private void selectTab(int index) throws Exception {
        Fx.run(() -> node("#tabs", TabPane.class).getSelectionModel().select(index));
    }

    private String status() throws Exception {
        return Fx.call(() -> node("#status", Label.class).getText());
    }

    private int rowsOnSelectedTab() throws Exception {
        return Fx.call(() -> ((TableView<?>) node("#tabs", TabPane.class)
                .getSelectionModel().getSelectedItem().getContent()).getItems().size());
    }

    private <N> N node(String id, Class<N> type) {
        return type.cast(scene.lookup(id));
    }

    private static KeyEvent enter() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "\r", "", KeyCode.ENTER, false, false, false, false);
    }
}

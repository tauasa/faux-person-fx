package org.tauasa.faux.app;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.springframework.stereotype.Component;

/**
 * The window. Its controls are built in {@link #show(Stage)}, on the FX
 * thread, not when Spring creates the bean, so the context starts without
 * building any of them.
 *
 * <p>Each control has an id ({@code #count}, {@code #region}, {@code #seed},
 * {@code #randomSeed},
 * {@code #realCompanies}, {@code #generate}, {@code #clear}, {@code #status},
 * {@code #tabs}, {@code #menuBar}) so tests can find it. Menu items have ids
 * too, found through the menu bar.
 */
@Component
public class MainView {

    static final String MIXED = "Mixed";

    private final FauxService service;
    private final FauxFxProperties properties;
    private final About about;
    private final SeedHistoryFile historyFile;
    private final AppearanceFile appearanceFile;
    private final SystemTheme systemTheme;
    private Appearance appearance = Appearance.SYSTEM;
    private final ToggleGroup appearanceGroup = new ToggleGroup();

    private final Map<Kind, Sheet<?>> sheets = new EnumMap<>(Kind.class);
    private TabPane tabs;
    private Spinner<Integer> count;
    private ComboBox<String> region;
    private ComboBox<String> seed;
    private Button randomSeed;
    private CheckBox realCompanies;
    private Button generate;
    private Button clear;
    private Label status;
    /** The selected tab's row count, in the middle of the tab row. */
    private Label rowCount;

    /** The tabs, left to right: People first, Names last. */
    static final List<Kind> TAB_ORDER = List.of(Kind.PEOPLE, Kind.COMPANIES, Kind.NAMES);
    private Stage stage;
    private Consumer<String> openLink;
    private MenuItem copy;
    private MenuItem save;
    private Path lastDirectory;
    /** The type File > Save starts on: zipped CSV, then whatever was saved last. */
    private ExportFormat.SaveType lastType = ExportFormat.SaveType.all().get(0);
    private MenuItem selectAll;
    private final ToggleGroup viewGroup = new ToggleGroup();

    public MainView(FauxService service, FauxFxProperties properties, About about, SystemTheme systemTheme) {
        this.service = service;
        this.properties = properties;
        this.about = about;
        this.systemTheme = systemTheme;
        this.historyFile = properties.historyPath().map(SeedHistoryFile::new).orElse(null);
        this.appearanceFile = properties.appearancePath().map(AppearanceFile::new).orElse(null);
    }

    /**
     * Builds the window and shows it. Call on the FX thread.
     *
     * @param stage    the window
     * @param openLink opens a URL in the browser, for Help > About
     */
    public void show(Stage stage, Consumer<String> openLink) {
        this.stage = stage;
        this.openLink = openLink;
        sheets.put(Kind.NAMES, new Sheet<>(Kind.NAMES, Columns.names(), service::names));
        sheets.put(Kind.COMPANIES, new Sheet<>(Kind.COMPANIES, Columns.companies(), service::companies));
        sheets.put(Kind.PEOPLE, new Sheet<>(Kind.PEOPLE, Columns.people(LocalDate.now()), service::people));
        if (historyFile != null) {
            historyFile.load().forEach((kind, runs) -> sheets.get(kind).history.restore(runs));
        }
        if (appearanceFile != null) {
            appearance = appearanceFile.load();
        }

        tabs = new TabPane();
        tabs.setId("tabs");
        for (Kind kind : TAB_ORDER) {
            Sheet<?> sheet = sheets.get(kind);
            Tab tab = new Tab(sheet.kind.title(), sheet.table.view());
            tab.setClosable(false);
            tab.setUserData(sheet.kind);
            tabs.getTabs().add(tab);
        }
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, was, now) -> {
            refreshControls();
            showSeedOf(selected());
        });
        for (Sheet<?> sheet : sheets.values()) {
            sheet.table.view().getSelectionModel().getSelectedIndices()
                    .addListener((javafx.collections.ListChangeListener<Integer>) c -> refreshEditMenu());
        }

        HBox toolbar = toolbar();
        BorderPane root = new BorderPane();
        root.setTop(new VBox(menuBar(), toolbar));
        // The row count sits on the tab row, centered. It lets clicks through,
        // so it never covers a tab.
        rowCount = new Label();
        rowCount.setId("rowCount");
        rowCount.setMouseTransparent(true);
        StackPane tabsAndCount = new StackPane(tabs, rowCount);
        StackPane.setAlignment(rowCount, Pos.TOP_CENTER);
        StackPane.setMargin(rowCount, new Insets(6, 0, 0, 0));
        root.setCenter(tabsAndCount);
        root.setBottom(statusBar());

        refreshControls();
        showSeedOf(selected());
        Scene scene = new Scene(root, 1100, 700);
        // Edit > Copy and Select All follow whether a text box has focus.
        scene.focusOwnerProperty().addListener((obs, was, now) -> refreshEditMenu());
        stage.setScene(scene);
        showDark(appearance.isDark(systemTheme.lastKnownIsDark()));
        // In System, pick up a change made in the OS while the app was in the background.
        stage.focusedProperty().addListener((obs, was, focused) -> {
            if (focused && appearance == Appearance.SYSTEM) {
                followSystem();
            }
        });
        // Style and lay out before showing, so the window opens whole rather
        // than painting blank and then filling in.
        root.applyCss();
        root.layout();
        stage.setTitle(About.NAME);
        stage.centerOnScreen();
        stage.show();
    }

    private HBox toolbar() {
        SpinnerValueFactory.IntegerSpinnerValueFactory counts = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, properties.maxCount(), properties.defaultCount());
        // The stock converter throws on text that isn't a number. This keeps
        // the last good value instead; generate() reports the bad text.
        counts.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer n) {
                return n == null ? "" : n.toString();
            }

            @Override
            public Integer fromString(String text) {
                try {
                    return Integer.valueOf(text.strip());
                } catch (NumberFormatException e) {
                    return counts.getValue();
                }
            }
        });
        count = new Spinner<>(counts);
        count.setId("count");
        count.setEditable(true);
        count.setPrefWidth(110);
        // The spinner's editor keeps Enter for itself, so the default button
        // never sees it there. Generate from here too.
        count.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                generate();
                e.consume();
            }
        });

        region = new ComboBox<>();
        region.setId("region");
        region.getItems().add(MIXED);
        Arrays.stream(org.tauasa.faux.model.Region.values()).map(Enum::name).sorted()
                .forEach(region.getItems()::add);
        region.getSelectionModel().select(MIXED);

        // The seed box always shows the last seed this tab used. Clicking it
        // lists the tab's earlier runs; choosing one restores its settings.
        seed = new ComboBox<>();
        seed.setId("seed");
        seed.setEditable(true);
        seed.setPromptText("random");
        seed.getEditor().setPrefColumnCount(14);
        seed.setVisibleRowCount(10);
        seed.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                Sheet<?> sheet = selected();
                setText(sheet.history.find(item).map(run -> item + "   " + run.describe(sheet.kind)).orElse(item));
            }
        });
        seed.getEditor().setOnMouseClicked(e -> {
            if (!seed.getItems().isEmpty() && !seed.isShowing()) {
                seed.show();
            }
        });
        // A pick from the list happens while it's open; typing doesn't restore settings.
        seed.valueProperty().addListener((obs, was, now) -> {
            if (seed.isShowing()) {
                selected().history.find(now).ifPresent(this::applyRun);
            }
        });
        seed.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !seed.isShowing()) {
                generate();
                e.consume();
            }
        });

        randomSeed = new Button("Random");
        randomSeed.setId("randomSeed");
        randomSeed.setTooltip(new Tooltip("Clear the seed, so the next Generate picks a new one"));
        randomSeed.setOnAction(e -> {
            seed.setValue(null);
            seed.getEditor().clear();
        });

        realCompanies = new CheckBox("Real companies");
        realCompanies.setId("realCompanies");

        // The default button takes Enter everywhere but the count and seed boxes,
        // which keep Enter for themselves and so call generate() directly.
        generate = new Button("Generate");
        generate.setId("generate");
        generate.setDefaultButton(true);
        generate.setOnAction(e -> generate());

        clear = new Button("Clear");
        clear.setId("clear");
        clear.setOnAction(e -> {
            selected().clear();
            refreshControls();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8,
                new Label("Count"), count,
                new Label("Region"), region,
                new Label("Seed"), seed, randomSeed,
                realCompanies, spacer, generate, clear);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8));
        return bar;
    }

    private MenuBar menuBar() {
        MenuItem generateItem = item("menuGenerate", "Generate", KeyCode.G, e -> generate());
        generateItem.disableProperty().bind(generate.disableProperty());
        save = item("menuSave", "Save\u2026", KeyCode.S, e -> chooseAndSave());
        MenuItem exit = item("menuExit", "Exit", null, e -> {
            stage.close();
            Platform.exit();
        });
        Menu file = new Menu("File", null, generateItem, save, new SeparatorMenuItem(), exit);

        copy = item("menuCopy", "Copy", KeyCode.C, e -> copy());
        selectAll = item("menuSelectAll", "Select All", KeyCode.A, e -> selectAll());
        MenuItem clearItem = item("menuClear", "Clear Tab", null, e -> clear.fire());
        clearItem.disableProperty().bind(clear.disableProperty());
        Menu edit = new Menu("Edit", null, copy, selectAll, new SeparatorMenuItem(), clearItem);
        edit.setOnShowing(e -> refreshEditMenu());

        Menu view = new Menu("View");
        KeyCode[] digits = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3};
        for (int i = 0; i < tabs.getTabs().size(); i++) {
            Tab tab = tabs.getTabs().get(i);
            RadioMenuItem show = new RadioMenuItem(tab.getText());
            show.setId("menuView" + tab.getText());
            show.setToggleGroup(viewGroup);
            show.setUserData(tab);
            if (i < digits.length) {
                show.setAccelerator(new KeyCodeCombination(digits[i], KeyCombination.SHORTCUT_DOWN));
            }
            show.setOnAction(e -> tabs.getSelectionModel().select(tab));
            view.getItems().add(show);
        }

        // Appearance is the last item in View, after the tabs.
        Menu appearanceMenu = new Menu("Appearance");
        appearanceMenu.setId("menuAppearance");
        for (Appearance choice : Appearance.values()) {
            RadioMenuItem option = new RadioMenuItem(choice.label());
            option.setId("menuAppearance" + choice.label());
            option.setToggleGroup(appearanceGroup);
            option.setUserData(choice);
            option.setSelected(choice == appearance);
            option.setOnAction(e -> setAppearance(choice));
            appearanceMenu.getItems().add(option);
        }
        view.getItems().addAll(new SeparatorMenuItem(), appearanceMenu);
        MenuItem aboutItem = item("menuAbout", "About " + About.NAME, null, e -> showAbout());
        Menu help = new Menu("Help", null, aboutItem);

        MenuBar bar = new MenuBar(file, edit, view, help);
        bar.setId("menuBar");
        // In the window on every platform. The macOS screen menu bar put the
        // menus under a "java" app name at the top of the display, where they
        // were easy to miss.
        bar.setUseSystemMenuBar(false);
        return bar;
    }

    private static MenuItem item(String id, String text, KeyCode shortcut,
            javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        MenuItem item = new MenuItem(text);
        item.setId(id);
        if (shortcut != null) {
            item.setAccelerator(new KeyCodeCombination(shortcut, KeyCombination.SHORTCUT_DOWN));
        }
        item.setOnAction(action);
        return item;
    }

    /** Copy and Select All act on a text box when one has focus, so its own copy still works. */
    private void copy() {
        if (focusedText() instanceof TextInputControl text) {
            text.copy();
            return;
        }
        String tsv = selected().table.selectedAsTsv();
        if (!tsv.isEmpty()) {
            ClipboardContent content = new ClipboardContent();
            content.putString(tsv);
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    private void selectAll() {
        if (focusedText() instanceof TextInputControl text) {
            text.selectAll();
            return;
        }
        selected().table.selectAll();
        refreshEditMenu();
    }

    /**
     * The text box with focus, if any. An editable combo box or spinner is
     * reported as focused itself, not its editor, so look inside those.
     */
    private Node focusedText() {
        Node focus = stage.getScene() == null ? null : stage.getScene().getFocusOwner();
        if (focus instanceof ComboBox<?> box && box.isEditable()) {
            return box.getEditor();
        }
        if (focus instanceof Spinner<?> spinner && spinner.isEditable()) {
            return spinner.getEditor();
        }
        return focus instanceof TextInputControl ? focus : null;
    }

    private void refreshEditMenu() {
        boolean text = focusedText() != null;
        copy.setDisable(!text && !selected().table.hasSelection());
        selectAll.setDisable(!text && selected().table.size() == 0);
    }

    /** File > Save: asks where, then writes the selected tab. */
    private void chooseAndSave() {
        Sheet<?> sheet = selected();
        if (sheet.running || sheet.table.size() == 0) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + sheet.kind.title());
        List<ExportFormat.SaveType> types = ExportFormat.SaveType.all();
        for (ExportFormat.SaveType type : types) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(type.description(), type.patterns()));
        }
        // Zipped CSV first and chosen, so a saved file comes with its data credit; after a
        // save, the type saved. The dialog can't rename as the type changes, so this is its one chance.
        chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().get(types.indexOf(lastType)));
        chooser.setInitialFileName(suggestedName());
        if (lastDirectory != null && Files.isDirectory(lastDirectory)) {
            chooser.setInitialDirectory(lastDirectory.toFile());
        }
        File chosen = chooser.showSaveDialog(stage);
        if (chosen == null) {
            return;
        }
        int filter = chooser.getExtensionFilters().indexOf(chooser.getSelectedExtensionFilter());
        save(chosen.toPath(), filter < 0 ? types.get(0) : types.get(filter));
    }

    /** The save dialog's suggested name for the selected tab, now, in the type it starts on. */
    String suggestedName() {
        return ExportFormat.suggestedName(selected().kind.title().toLowerCase(), LocalDateTime.now(), lastType);
    }

    /** {@link #save(Path, ExportFormat.SaveType)}, unzipped unless the name ends {@code .zip}. */
    Path save(Path chosen, ExportFormat fallback) {
        return save(chosen, new ExportFormat.SaveType(fallback, false));
    }

    /**
     * Writes the selected tab's rows, in the order shown, as UTF-8, and says
     * so in the status bar. The file's name picks the format and whether it's
     * zipped, as {@link ExportFormat#resolve(Path, ExportFormat.SaveType)}
     * says; a zip holds the rows and {@link Exporter#NOTICE_NAME}.
     *
     * @return the file written, or {@code null} if writing failed
     */
    Path save(Path chosen, ExportFormat.SaveType fallback) {
        Sheet<?> sheet = selected();
        ExportFormat.Target target = ExportFormat.resolve(chosen, fallback);
        try {
            if (target.zip()) {
                Exporter.zip(target.file(), target.entryName(), out -> sheet.table.export(target.format(), out));
            } else {
                try (Writer out = Files.newBufferedWriter(target.file(), StandardCharsets.UTF_8)) {
                    sheet.table.export(target.format(), out);
                }
            }
        } catch (IOException e) {
            status.setText("Couldn't save " + target.file().getFileName() + ": " + e.getMessage());
            return null;
        }
        lastDirectory = target.file().toAbsolutePath().getParent();
        lastType = new ExportFormat.SaveType(target.format(), target.zip());
        status.setText("Saved " + sheet.kind.count(sheet.table.size()) + " to " + target.file().getFileName()
                + " as " + target.label());
        return target.file();
    }

    /** View > Appearance: switches now and remembers the choice for the next session. */
    void setAppearance(Appearance choice) {
        appearance = choice;
        appearanceGroup.getToggles().forEach(t -> t.setSelected(t.getUserData() == choice));
        if (appearanceFile != null) {
            appearanceFile.save(choice);
        }
        showDark(choice.isDark(systemTheme.lastKnownIsDark()));
        if (choice == Appearance.SYSTEM) {
            followSystem();
        }
    }

    /** The current choice. */
    Appearance appearance() {
        return appearance;
    }

    /** Asks the system for light or dark off the FX thread, then shows it if still in System. */
    private void followSystem() {
        Thread ask = new Thread(() -> {
            boolean dark = systemTheme.isDark();
            Platform.runLater(() -> {
                if (appearance == Appearance.SYSTEM) {
                    showDark(dark);
                }
            });
        }, "faux-system-theme");
        ask.setDaemon(true);
        ask.start();
    }

    /** Dialogs and popups copy the window's stylesheets, so they follow it. */
    private void showDark(boolean dark) {
        List<String> styles = stage.getScene().getStylesheets();
        if (dark && !styles.contains(Appearance.DARK_STYLESHEET)) {
            styles.add(Appearance.DARK_STYLESHEET);
        } else if (!dark) {
            styles.remove(Appearance.DARK_STYLESHEET);
        }
    }

    /** Whether the window shows dark now. */
    boolean showsDark() {
        return stage.getScene().getStylesheets().contains(Appearance.DARK_STYLESHEET);
    }

    private void showAbout() {
        AboutDialog.create(stage, about, openLink).showAndWait();
    }

    private HBox statusBar() {
        status = new Label();
        status.setId("status");
        HBox bar = new HBox(status);
        bar.setPadding(new Insets(4, 8, 4, 8));
        return bar;
    }

    private void generate() {
        Sheet<?> sheet = selected();
        if (sheet.running) {
            return;
        }
        GenerateRequest request;
        try {
            request = new GenerateRequest(
                    readCount(),
                    MIXED.equals(region.getValue()) ? null : org.tauasa.faux.model.Region.valueOf(region.getValue()),
                    GenerateRequest.parseSeed(seed.getEditor().getText()),
                    realCompanies.isSelected());
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
            return;
        }
        sheet.generate(request);
    }

    /** The spinner only takes typed text on Enter or focus loss, so read the text here. */
    private int readCount() {
        String text = count.getEditor().getText().strip();
        int n;
        try {
            n = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Count must be a whole number from 1 to " + properties.maxCount());
        }
        if (n < 1 || n > properties.maxCount()) {
            throw new IllegalArgumentException("Count must be from 1 to " + properties.maxCount());
        }
        count.getValueFactory().setValue(n);
        return n;
    }

    /** The seed box shows the tab's last seed, and lists its runs, most recent first. */
    private void showSeedOf(Sheet<?> sheet) {
        seed.getItems().setAll(sheet.history.runs().stream().map(r -> Long.toString(r.seed())).toList());
        String last = sheet.history.last().map(r -> Long.toString(r.seed())).orElse(null);
        seed.setValue(last);
        seed.getEditor().setText(last == null ? "" : last);
    }

    /** Writes every tab's history, so the next session has it. */
    private void saveHistory() {
        if (historyFile == null) {
            return;
        }
        Map<Kind, SeedHistory> histories = new EnumMap<>(Kind.class);
        sheets.forEach((kind, sheet) -> histories.put(kind, sheet.history));
        historyFile.save(histories);
    }

    /** Puts a past run's settings back, so Generate makes the same rows again. */
    private void applyRun(SeedHistory.Run run) {
        String text = Long.toString(run.seed());
        seed.getEditor().setText(text);
        count.getValueFactory().setValue(run.count());
        count.getEditor().setText(Integer.toString(run.count()));
        region.getSelectionModel().select(run.region() == null ? MIXED : run.region().name());
        realCompanies.setSelected(run.realCompanies());
        status.setText("Seed " + text + " chosen: " + run.describe(selected().kind) + ". Press Generate to make it again.");
    }

    private Sheet<?> selected() {
        Tab tab = tabs.getSelectionModel().getSelectedItem();
        return sheets.get((Kind) tab.getUserData());
    }

    private void refreshControls() {
        Sheet<?> sheet = selected();
        region.setDisable(sheet.kind == Kind.COMPANIES);
        realCompanies.setDisable(sheet.kind == Kind.NAMES);
        generate.setDisable(sheet.running);
        clear.setDisable(sheet.running || sheet.table.size() == 0);
        status.setText(sheet.describe());
        if (rowCount != null) {
            rowCount.setText(sheet.kind.count(sheet.table.size()));
        }
        Tab tab = tabs.getSelectionModel().getSelectedItem();
        viewGroup.getToggles().stream().filter(t -> t.getUserData() == tab).findFirst()
                .ifPresent(t -> t.setSelected(true));
        if (copy != null) {
            refreshEditMenu();
            save.setDisable(sheet.running || sheet.table.size() == 0);
        }
    }

    /** One tab: its table, how it generates, and the state behind its status line. */
    private final class Sheet<T> {
        final Kind kind;
        final ResultsTable<T> table;
        final Function<GenerateRequest, Generated<T>> generator;
        Long seed;
        final SeedHistory history = new SeedHistory();
        boolean running;
        int pending;
        String error;

        Sheet(Kind kind, List<Column<T>> columns, Function<GenerateRequest, Generated<T>> generator) {
            this.kind = kind;
            this.table = new ResultsTable<>(columns, Columns.defaultSort(kind));
            if (kind == Kind.COMPANIES || kind == Kind.PEOPLE) {
                table.setOnRowOpen(this::open);
            }
            this.generator = generator;
        }

        void generate(GenerateRequest request) {
            Task<Generated<T>> task = new Task<>() {
                @Override
                protected Generated<T> call() {
                    return generator.apply(request);
                }
            };
            task.setOnSucceeded(e -> {
                Generated<T> result = task.getValue();
                table.setRows(result.rows());
                seed = result.seed();
                history.add(SeedHistory.Run.of(request, result.seed()));
                saveHistory();
                running = false;
                refreshControls();
                if (selected() == this) {
                    showSeedOf(this);
                }
            });
            task.setOnFailed(e -> {
                running = false;
                error = "Couldn't generate " + kind.title().toLowerCase() + ": " + task.getException().getMessage();
                refreshControls();
            });
            running = true;
            pending = request.count();
            error = null;
            refreshControls();
            Thread worker = new Thread(task, "faux-generate");
            worker.setDaemon(true);
            worker.start();
        }

        /** A double-clicked row, in a modal dialog of its own. */
        void open(T row) {
            Dialog<Void> dialog = RecordDialog.create(stage, kind, heading(row), table.columns(), row);
            // show(), not showAndWait(): it's modal either way, and the FX thread stays free.
            dialog.show();
        }

        private String heading(T row) {
            if (row instanceof org.tauasa.faux.model.Person person) {
                return person.name().fullName();
            }
            if (row instanceof org.tauasa.faux.model.Company company) {
                return company.name();
            }
            return String.valueOf(row);
        }

        void clear() {
            table.clear();
            seed = null;
            error = null;
        }

        String describe() {
            if (running) {
                return "Generating " + kind.count(pending) + "...";
            }
            if (error != null) {
                return error;
            }
            if (table.size() == 0) {
                return "No " + kind.title().toLowerCase() + " yet.";
            }
            return kind.count(table.size()) + ", seed " + seed;
        }
    }
}

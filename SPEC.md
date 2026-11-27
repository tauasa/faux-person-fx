# faux-person-fx: specification

A desktop application that generates random names, companies and people with
[faux-person](https://github.com/tauasa/faux-person) and shows them in tables
that sort by any column.

| | |
|---|---|
| Coordinates | `org.tauasa:faux-person-fx:0.4.0-SNAPSHOT`, released as jars on GitHub, not on Maven Central |
| Package | `org.tauasa.faux.app` |
| Java | 17 (`maven.compiler.release=17`) |
| Build | Maven, with `spring-boot-starter-parent` |
| Dependencies | `org.tauasa:faux-person:0.4.0`, Spring Boot 4.2 (core only, no web server), JavaFX 21 LTS (`javafx-controls`). Spring Boot's test starter at test scope |
| Status | Draft, 2026-11-21 |

## Goals

1. Generate **names**, **companies** or **people** with one click, and see
   them in a table.
2. **Every column sorts**, ascending or descending, by clicking its header.
   Shift-click adds a second sort key.
3. **Names and people open sorted by last name.** Companies have no last
   name, so they open sorted by company name.
4. **Every field faux-person makes is a column**, so nothing it generates is
   hidden.
5. **Reproducible.** A seed typed in gives the same rows every time. When the
   seed is left blank, the app still shows the one it used, so the run can be
   replayed.
6. The UI stays responsive while it generates: generation runs off the FX
   thread.

## Non-goals

- Saving rows to a database. faux-person-jpa shows that. File > Save writes
  files.
- Editing rows.
- A web or REST layer. Spring Boot runs with `web-application-type=none`.
- Installers or native packages. The fat jar carries the JavaFX build for the
  platform it was built on.

## 1. Architecture

Spring owns the objects. JavaFX owns the window.

| Class | Does |
|---|---|
| `Launcher` | Plain `main()`, which names `SplashPreloader` in the `javafx.preloader` property (unless it's already set) and calls `Application.launch(FauxFxApp.class)`. A fat jar whose main class extends `Application` won't start JavaFX from the classpath, so the launcher is a separate class |
| `SplashPreloader` | The JavaFX `Preloader`: an undecorated window with the name, a progress bar and a line saying what's happening. The bar never moves backwards. It stays up through `BEFORE_START` and closes on `Splash.Ready`, or on a startup error |
| `Splash` | The notifications the app sends the splash: `Progress(fraction, message)` and `Ready` |
| `StartupProgress` | A Spring `ApplicationListener` that turns Boot's startup events, from starting to ready, into steps from 0.05 to 0.6 |
| `ExportFormat` | CSV, pipe and JSON, their extensions, and which one a file name picks |
| `Exporter` | Writes columns and rows in a format. Plain Java, no JavaFX |
| `FauxFxApp` | The JavaFX `Application`. `init()` starts the Spring context (not headless, no web) with `StartupProgress` listening, then has `FauxService.warmUp()` make one of each kind, so faux-person reads its data files behind the splash rather than on the first Generate. `start(stage)` asks the context for `MainView` and shows it with the app's `HostServices` to open links, then sends `Splash.Ready` on the next pulse, once the window has painted. `stop()` closes the context |
| `FauxFxApplication` | The `@SpringBootApplication` configuration class |
| `FauxFxProperties` | `@ConfigurationProperties("faux.fx")`: `default-count` (100), `max-count` (10000) and `history-file` (`~/.faux-person-fx/seed-history.txt`; blank keeps no history on disk) |
| `FauxService` | `@Service` that turns a `GenerateRequest` into a `Generated<T>`: the rows and the seed used |
| `GenerateRequest` | Record: `count`, `region` (`null` for mixed), `seed` (`null` for random), `realCompanies` |
| `Columns` | The column definitions for each kind, and which column each sorts by first. Plain Java, no JavaFX, so the tests run headless |
| `Column<T>` | Record: a title and a function from a row to a `Comparable` value. Its comparator sorts strings ignoring case, then by exact text, so the order is stable |
| `ResultsTable<T>` | Builds a JavaFX `TableView<T>` from a list of columns: a `SortedList` bound to the table's comparator, the default sort column set ascending |
| `SeedHistory` | One tab's runs, most recent first: each run's seed, count, region and real-companies switch, one entry per seed, at most 20. Plain Java |
| `SeedHistoryFile` | Reads and writes every tab's `SeedHistory` as text, `tab,seed,count,region,realCompanies` per line, newest first. Skips lines it can't read; writes beside the file and moves it over, so a crash leaves the old file; logs a failure to read or write and carries on |
| `Appearance` | Light, Dark or System, and whether each shows dark given the OS's setting. Dark is `dark.css`, a stylesheet over Modena's light theme that recolors the colors Modena derives the rest from |
| `SystemTheme` | `@Component` that asks the OS whether it's set to dark, since JavaFX 21 can't: `defaults read -g AppleInterfaceStyle` on macOS, `AppsUseLightTheme` in the registry on Windows, GNOME's `color-scheme` or GTK theme on Linux. Anything it can't read is light. Remembers its last answer; asked once behind the splash |
| `AppearanceFile` | Keeps the Appearance choice as one word in `appearance-file`. Missing or unreadable means System. Also read by the splash, before Spring, from `-Dfaux.fx.appearance-file` or the default |
| `RecordDialog` | One row in a modal dialog owned by the window: a heading (the person's full name or the company's name), then every column's title and value, top to bottom, the values in read-only text boxes that can be selected and copied |
| `MainView` | `@Component` that builds the window, menus included, when `show(stage, openLink)` runs, not when the bean is created, so the Spring context starts without building it |
| `About` | `@Component` with the app's and faux-person's versions, which Maven writes into `application.properties` at build time |
| `AboutDialog` | Builds Help > About: a modal dialog with the project, its license, the licenses of what it's built with, the credit faux-person's data needs, and a link to the GitHub project |

## 2. The window

```
+--------------------------------------------------------------------+
| File  Edit  View  Help                                             |
+--------------------------------------------------------------------+
| Count [100 ^v]  Region [Mixed v]  Seed [8812345507791 v] [Random]  |
| [x] Real companies                                                 |
|                                         [ Generate ]  [ Clear ]    |
+--------------------------------------------------------------------+
| [ Names ] [ Companies ] [ People ]                                 |
| +----------------------------------------------------------------+ |
| | First name | Last name ^ |                                     | |
| | ...                                                            | |
| +----------------------------------------------------------------+ |
+--------------------------------------------------------------------+
| 100 names, seed 8812345507791                                      |
+--------------------------------------------------------------------+
```

- **Generate** fills the selected tab's table, replacing what it showed. The
  other tabs keep their rows.
- **Clear** empties the selected tab's table.
- **Region** lists `Mixed` and every faux-person region. Companies have no
  region, so it's disabled on the Companies tab.
- **Real companies** is faux-person's switch: off gives `[faux]` employers
  under `.example`. Names have no company, so it's disabled on the Names tab.
- **Seed** accepts any `long`. Anything else shows an error in the status bar
  and generates nothing. Enter generates, from the seed box, the count box or
  anywhere else in the window.
- **The seed box always shows the last seed the selected tab used.** A blank
  box means a random seed, and once the run finishes the box shows the seed
  it picked. Switching tabs shows that tab's last seed, or nothing if it hasn't
  run. Clear empties the table but leaves the seed, so the run can be made
  again.
- **Clicking the seed box lists the tab's earlier runs**, most recent first,
  each with its settings: `5   12 people, JPN, real companies`. Choosing one
  puts back its seed, count, region and real-companies switch, so Generate
  makes the same rows again. It doesn't generate by itself. Typing a seed
  changes only the seed. Each tab keeps its own list, of up to 20 seeds, one
  entry per seed. The lists are saved to `history-file` after every run and
  read back when the window opens, so the next session starts with them and
  each tab's seed box shows its last seed.
- **Random** clears the seed box, so the next Generate picks a new seed.
  Without it, Generate with the box as it stands repeats the last run's seed.
- While a tab is generating, its Generate is disabled and its status says so.
  A failure stays on that tab's status line.
- **Count** runs from 1 to `max-count`.
- The status bar says how many rows the selected tab holds and the seed that
  made them. The same count ("25 people") is centered on the tab row, level
  with the tabs; it lets clicks through.
- The tabs run People, Companies, Names, and the window opens on People.
- Rows can be selected several at a time.
- **Double-clicking a row on the Companies or People tab** opens it in a
  `RecordDialog`, titled `Person: Jane Doe` or `Company: Costco [faux]`.
  Names have two fields, both in the table, so double-clicking one does
  nothing. Neither does a single click, another button, or the empty space
  below the rows.

### Menus

| Menu | Item | Shortcut | Does |
|---|---|---|---|
| File | Generate | Shortcut+G | As the Generate button, and disabled when it is |
| File | Save… | Shortcut+S | Asks for a file, then writes every row of the selected tab, in the order shown. Disabled while the tab is empty or generating |
| File | Exit | | Closes the window and exits |
| Edit | Copy | Shortcut+C | The selected rows, in the order shown, as tab-separated text with a header line. Tabs and line breaks inside a value become spaces. In a focused text box, copies its text instead. Disabled when there's nothing to copy |
| Edit | Select All | Shortcut+A | Every row of the tab, or a focused text box's text |
| Edit | Clear Tab | | As the Clear button, and disabled when it is |
| View | People, Companies, Names | Shortcut+1, 2, 3 | Selects that tab. The checked item follows the selected tab |
| View | Appearance > Light, Dark, System | | Switches the stylesheet now and saves the choice. The checked item is the current choice. System asks the OS off the FX thread, again each time the window regains focus |
| Help | About faux-person-fx | | The About dialog |

Shortcut is Cmd on macOS and Ctrl elsewhere. The menu bar is in the window
on every platform, macOS included: in the screen's menu bar it sat under a
`java` app name, where it was easy to miss. Copy and Select All re-check when focus moves, so they're enabled
in a text box even when the tab is empty.

### Saving

The file's extension picks the format (`csv`; `psv` or `txt`; `json`, any
case). A name without one is saved in the type chosen in the dialog, with its
first extension added. UTF-8, no byte-order mark. The status bar says what was
saved where, or why it couldn't be.

| Format | Rules |
|---|---|
| CSV | RFC 4180: a header of column titles, CRLF line ends, a field quoted when it holds a comma, a quote, CR or LF, with quotes doubled |
| Pipe | As CSV with `\|` for the comma, and LF line ends |
| JSON | An array of objects, one per row, two-space indent. Keys are the column titles in camelCase. Integers are numbers, dates ISO-8601 strings, everything else strings |

### Appearance

View > Appearance, the last item in View, holds Light, Dark and System, with System the default. The
window opens in the saved choice. Dialogs and popups copy the window's
stylesheets, so they follow it. The splash reads the saved choice too, so a
dark app doesn't open with a light splash.

### Splash screen

Shown from launch until the main window has painted. The bar moves through
Spring's startup (to 0.6), loading faux-person's data (to 0.9) and building
the window (to 1), with a line saying which. The main window is styled and
laid out before it's shown, so it opens whole.

### About

A modal dialog, owned by the window, showing:

- the name, `faux-person-fx`, and its version
- what it does, in one line
- a link to https://github.com/tauasa/faux-person-fx, opened in the browser
- below it, a link to the author's website, https://tauasa.org
- `MIT License. Copyright (c) 2026 Tauasa Timoteo.`
- what it's built with, as the jar carries them: faux-person (MIT); JavaFX
  (GPL v2 with the Classpath Exception); Spring Boot, Spring Framework,
  Micrometer, Log4j API, SnakeYAML, Commons Logging and JSpecify (Apache
  2.0); Logback (EPL 1.0 or LGPL 2.1); SLF4J (MIT); Jakarta Annotations (EPL
  2.0 or GPL v2 with the Classpath Exception)
- where faux-person's data comes from, worded as its NOTICE words it:
  Wikidata (CC0 1.0); GeoNames under CC BY 4.0, with the changes made to it,
  which that license asks for; and US Census Bureau TIGER/Line, not subject
  to copyright in the United States
- the version shows `dev` when the build didn't fill it in, as in an IDE run

## 3. Columns

| Kind | Columns | Sorted first by |
|---|---|---|
| Names | First name, Last name, Native name | Last name |
| Companies | Name, City, State, Country, Domain | Name |
| People | First name, Last name, Native name, Gender, Region, Date of birth, Age, Email, Phone, Street, City, State, Postal code, Country, Employer, Employer city, Employer state, Employer country, Employer domain | Last name |

Native name is the whole name in its own script, as faux-person gives it:
Hangul for South Korean names (전영민), and from 0.4.0 kanji, Cyrillic, Greek,
Hebrew, Arabic and Persian for those regions' names that Wikidata spells in
them; blank otherwise. Country (a company's, or a person's employer's) is the
headquarters' ISO 3166-1 code; State is blank outside the US. faux-person 0.4.0
adds 4,000 international companies, and a person works for one in their
region's countries. These reach the copy, the saved files (`nativeName`,
`country` and `employerCountry` in JSON) and the record dialog, like any column.

Date of birth sorts as a date and Age as a number, not as text. Gender and
Region show their names and sort A to Z, and the Region list is A to Z after
`Mixed`. Age is worked
out from the date the window opened.

## 4. Tests

`mvn verify` runs them. `ResultsTableTest`, `MainViewTest`, `AboutDialogTest` and
`SplashPreloaderTest` start the JavaFX toolkit, so they need a display: CI runs them under `xvfb`. The rest
run headless.

| Area | Fails when |
|---|---|
| The same seed gives the same rows, for each kind | Reproducibility breaks |
| A blank seed is reported, and replaying it gives the same rows | The status bar's seed can't replay a run |
| Count and region are honored | The request is ignored |
| Companies and people's employers are under `.example` unless real companies is on | The faux marker is lost |
| Every column sorts both ways in its field's own order, written in the test from the record, not from `Column` | A column sorts wrongly |
| Names and people sort first by last name, companies by name, in the real `TableView` | The default sort is wrong |
| Choosing another column in the real `TableView` re-sorts the rows | Header sorting is broken |
| Every field of `Person`, `Name`, `Address` and `Company`, read from the records, has its own column | A field is hidden, now or when faux-person adds one |
| Dates and ages are values, not text | `9` sorts after `10` |
| The window: Generate fills the selected tab and reports the seed, other tabs keep their rows, Clear empties only the selected tab, controls that don't apply are disabled | The window misbehaves |
| A bad seed or count is reported and generates nothing, and Enter in the Count box generates without throwing on junk | Bad input crashes or is ignored |
| The menu bar is File, Edit, View, Help. File > Generate and Edit > Clear Tab act on the selected tab, the View items and the tabs stay in step, and Copy is disabled until there are rows to copy | The menus are missing or act on the wrong tab |
| Copy gives the selected rows in the order shown, as tab-separated text with a header, and nothing when nothing is selected | Pasted rows land in the wrong cells or order |
| About is modal and owned by the window, shows the version, the MIT license and the GeoNames credit, and its link opens the GitHub project | The license or the link is wrong |
| The versions About shows are the build's | `application.properties` isn't filtered |
| CSV and pipe quote exactly what RFC 4180 says, with their line ends; JSON parses back to the same values, with every column, Age a number and dates ISO | A saved file reads back wrong |
| The extension picks the format, and a name without one gets the chosen type's | A file is saved in the wrong format or name |
| Save from the window writes the selected tab in the shown order, and a failed save says so | Save writes the wrong rows, or fails silently |
| Select All selects every row; Copy puts them on the clipboard; in a focused text box both act on its text, and Copy is enabled there | Edit acts on the wrong thing |
| The splash shows progress, never moves backwards, stays up through `BEFORE_START`, and closes on `Ready` or an error | The splash hangs, or the window flashes in before it closes |
| A real Spring start moves the bar forward at each step to 0.6 | The bar stalls or jumps back |
| A run's seed, blank or typed, shows in the seed box; each tab shows its own last seed, and Clear keeps it | The box doesn't say which seed made the rows |
| The list holds the tab's seeds, most recent first, one per seed, at most 20, and no other tab's | A run can't be found again, or another tab's leaks in |
| Clicking the box opens the list once there's something in it | The history can't be reached |
| Choosing a past seed restores its count, region and real-companies switch, and Generate then remakes the same rows; typing a seed changes nothing else | A sample can't be reproduced, or typing resets settings |
| Random clears the box and the next run picks a new seed; Enter in the box generates once | A new random run needs more than a click |
| History saved and loaded back is the same, newest first, per tab; a missing file is an empty history; unreadable lines are skipped and the rest kept; at most 20 per tab load | A session's seeds are lost or garbled |
| Saving replaces the file with no temp file left, and a file that can't be written doesn't throw | A crash corrupts the history, or a bad path breaks Generate |
| A second window on the same file opens with each tab's seeds and last seed | History isn't carried to the next session |
| `history-file` expands `~/`, and blank turns it off | The history lands in the wrong place, or can't be turned off |
| View ends with Appearance (Light, Dark, System), below the tabs, with System checked to start; Help holds About | The choice can't be found |
| The tabs run People, Companies, Names and the window opens on People; the row count on the tab row follows generating, clearing and switching, and is centered | The tabs or the count move |
| Light and Dark switch the window's stylesheet and the checked item; System follows the OS, asking again when chosen; Light ignores a dark OS | The window shows the wrong appearance |
| About opened from a dark window is dark | Dialogs ignore the appearance |
| The choice is saved and the next window opens in it; a missing or unreadable file means System | The choice isn't remembered |
| The splash opens in the saved appearance | The splash flashes light before a dark app |
| The OS check reads each system's setting, treats a failed or missing command as light, and asks once until told to ask again | System guesses wrong, or hangs on a missing command |
| Double-clicking a person or company row opens one modal dialog, owned by the window, with every column's value, read-only | The record can't be seen on its own, or shows wrong or editable values |
| Double-clicking a name, or single-clicking any row, opens nothing | Dialogs open by accident |
| The Spring context starts without building the window | Startup builds controls before the FX toolkit is up |

## 5. Running

- `./run.sh` (bash) and `./run.ps1` (PowerShell) build the fat jar when it's
  missing or older than `pom.xml` or anything under `src/`, then start it.
  `--rebuild` (`-Rebuild` in PowerShell) forces `mvn clean package`. Other
  arguments go to the app. `FAUX_FX_JAVA_OPTS` goes to the JVM. Both skip the
  tests, refuse a `java` older than 17, and work from any folder. run.sh
  follows a symlink to itself. run.ps1 has no `param()` block, so PowerShell
  doesn't match app arguments against it, and it leaves the caller's folder
  as it was.
- `mvn spring-boot:run` runs it from source.
- `mvn package`, then `java -jar target/faux-person-fx.jar`, runs the fat
  jar.

- `./release-jars.sh` builds one jar per platform for a release
  (RELEASING.md).

## 6. Repo and CI

- `.githooks/pre-commit` from Sanvio Factory, with `core.hooksPath` set.
- `.github/workflows/build.yml` runs `mvn -B verify` on JDK 17, for when it's
  pushed.

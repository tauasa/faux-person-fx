# faux-person-fx

A desktop app that generates random names, companies and people with
[faux-person](https://github.com/tauasa/faux-person) and shows them in tables
you can sort by any column. Built with Java 17, Spring Boot and JavaFX.

## Run it

Needs JDK 17 or later on `PATH`, and Maven. JavaFX comes from Maven, so
there's nothing else to install. Both scripts refuse an older `java` with a
message saying so.

```bash
./run.sh              # macOS / Linux, from bash or zsh
./run.ps1             # from a PowerShell prompt (Windows PowerShell 5.1 or PowerShell 7)
pwsh run.ps1          # PowerShell 7 from another shell
```

Each builds the fat jar on the first run, and again whenever the source is
newer than the jar, then starts it. Add `--rebuild` (or `-Rebuild` in
PowerShell) to force a clean build. Any other arguments go to the app, such
as `--faux.fx.max-count=50000`. Set `FAUX_FX_JAVA_OPTS` (e.g. `-Xmx2g`) to
pass options to the JVM. The scripts skip the tests to start faster; `mvn
verify` runs them. If PowerShell blocks the script, run
`powershell -ExecutionPolicy Bypass -File run.ps1`, or
`pwsh -ExecutionPolicy Bypass -File run.ps1`.

A splash screen with a progress bar shows while Spring starts and
faux-person loads its name and place data, and closes once the main window
is up.

Or drive Maven directly:

```bash
mvn spring-boot:run                 # from source
# or
mvn package
java -jar target/faux-person-fx.jar # the fat jar
```

The fat jar holds the JavaFX build for the platform that built it, so build it
on the platform you'll run it on. JavaFX prints `Unsupported JavaFX
configuration: classes were loaded from 'unnamed module'` on start. That's
expected for a classpath jar and does no harm.

## Use it

1. Pick a tab: **Names**, **Companies** or **People**.
2. Set **Count**, and for names or people a **Region** (`Mixed` draws from
   all of them).
3. Leave **Seed** blank for random rows, or type one. Once the rows are made
   the seed box shows the seed behind them, and each tab shows its own.
   Generate with the box as it stands repeats that seed; press **Random** to
   clear it for a new one.
4. **Click the seed box** to see that tab's earlier runs, newest first, each
   with its count and settings. Choose one and it puts the seed, count,
   region and real-companies switch back, then press Generate to make the
   same rows again. The list is kept between sessions (see Settings).
5. Tick **Real companies** for real employer names and email domains. Left
   off, employers are marked `[faux]` and live under `.example`, which never
   delivers mail.
6. Press **Generate** (or Enter). It replaces that tab's rows. The other
   tabs keep theirs.

### Menus

The menu bar is at the top of the window, on every platform. Shortcuts use
Cmd on macOS and Ctrl elsewhere.

| Menu | Item | Does |
|---|---|---|
| File | Generate (Shortcut+G) | Same as the Generate button |
| File | Save… (Shortcut+S) | Saves every row of the current tab, in the order shown, as CSV, pipe-delimited or JSON. See below |
| File | Exit | Closes the app |
| Edit | Copy (Shortcut+C) | Copies the selected rows, with a header line, as tab-separated text that pastes into a spreadsheet as columns. In a text box, copies its text instead |
| Edit | Select All (Shortcut+A) | Selects every row in the tab, or all the text in a text box |
| Edit | Clear Tab | Same as the Clear button |
| View | Names, Companies, People (Shortcut+1, 2, 3) | Switches tab |
| Help | About faux-person-fx | The version, the license, the licenses of what it's built with, and a link to the GitHub project |

On macOS the screen's menu bar shows JavaFX's own application menu, named
`java`, with Quit (Cmd+Q); the app's menus are in its window. Edit has Copy and Select All but no Cut, Paste or Undo, since rows
can't be edited.

### Saving

File > Save… asks for a file name and a type. The extension picks the
format: `.csv`, `.psv` or `.txt` (pipe-delimited), or `.json`. A name with
none of these is saved in the type chosen in the dialog, with its extension
added. Files are UTF-8 with no byte-order mark.

| Format | What's in it |
|---|---|
| CSV | A header row of column titles, then one line per row. Quoted as RFC 4180 says: a field holding a comma, a quote or a line break goes in double quotes, with its quotes doubled. CRLF line ends |
| Pipe | The same, with `\|` in place of the comma, and LF line ends |
| JSON | An array of objects, one per row, keyed by the column title in camelCase (`lastName`, `dateOfBirth`, `employerDomain`). Age is a number, dates are `YYYY-MM-DD` strings, everything else is a string |

Excel can misread non-English names in a UTF-8 CSV opened by double-click.
Data > From Text/CSV reads them correctly; Numbers, LibreOffice and Google
Sheets read them as they are.

Click a column header to sort by it, again to reverse it, and shift-click to
add another sort key. Names and people open sorted by last name, and
companies by name.

## Settings

`src/main/resources/application.properties`, or any Spring Boot way of
setting a property, such as `--faux.fx.max-count=50000` on the command line:

| Property | Default | Does |
|---|---|---|
| `faux.fx.default-count` | 100 | The count the window opens with |
| `faux.fx.max-count` | 10000 | The most rows one Generate may make |
| `faux.fx.history-file` | `~/.faux-person-fx/seed-history.txt` | Where each tab's seed history is kept between sessions. `~/` is your home folder. Set it empty (`--faux.fx.history-file=`) to keep no history on disk |

The history file is plain text, one run per line: the tab, seed, count,
region and real-companies switch. It's rewritten after every run and read
when the window opens, holds up to 20 seeds per tab, and is safe to delete.
A line it can't read is skipped. If two copies of the app run at once, the
last one to generate writes the file.

## Build and test

```bash
mvn verify
```

`ResultsTableTest`, `MainViewTest`, `AboutDialogTest` and `SplashPreloaderTest`
start the JavaFX toolkit, so they need a display. CI runs them under `xvfb`.

[SPEC.md](SPEC.md) has the design: the classes, the columns and what each test
protects.

## License

MIT. See [LICENSE](LICENSE).

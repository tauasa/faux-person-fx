package org.tauasa.faux.app;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The file formats File > Save writes. */
public enum ExportFormat {
    CSV("CSV", List.of("csv")),
    PIPE("Pipe-delimited", List.of("psv", "txt")),
    JSON("JSON", List.of("json"));

    private final String description;
    private final List<String> extensions;

    ExportFormat(String description, List<String> extensions) {
        this.description = description;
        this.extensions = extensions;
    }

    /** "CSV", for the save dialog's file types. */
    public String description() {
        return description;
    }

    /** The extensions it's saved under, without the dot. The first is the default. */
    public List<String> extensions() {
        return extensions;
    }

    /** The format a file name's extension names, if it names one. Case doesn't matter. */
    public static Optional<ExportFormat> forFileName(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = name.substring(dot + 1);
        for (ExportFormat format : values()) {
            if (format.extensions.contains(extension)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    /**
     * The file to write and its format, unzipped. A name whose extension names
     * a format is saved in that format. Otherwise it's saved as
     * {@code fallback}, with its default extension added.
     */
    public static Target resolve(Path chosen, ExportFormat fallback) {
        return resolve(chosen, new SaveType(fallback, false));
    }

    /**
     * The file to write, its format, and whether it's zipped. A name ending
     * {@code .zip} is zipped, and the name inside it picks the format, or
     * {@code fallback} does, with its extension added before {@code .zip}. A
     * name whose extension names a format is that format, unzipped. Any other
     * name is {@code fallback}, with its extensions added.
     */
    public static Target resolve(Path chosen, SaveType fallback) {
        String name = chosen.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(ZIP) && name.length() > ZIP.length()) {
            String innerName = name.substring(0, name.length() - ZIP.length());
            Optional<ExportFormat> inner = forFileName(chosen.resolveSibling(innerName));
            if (inner.isPresent()) {
                return new Target(chosen, inner.get(), true); // the name as typed, .ZIP included
            }
            SaveType zipped = new SaveType(fallback.format(), true);
            return new Target(chosen.resolveSibling(innerName + zipped.extension()), fallback.format(), true);
        }
        Optional<ExportFormat> named = forFileName(chosen);
        if (named.isPresent()) {
            return new Target(chosen, named.get(), false);
        }
        return new Target(chosen.resolveSibling(name + fallback.extension()), fallback.format(), fallback.zip());
    }

    private static final String ZIP = ".zip";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm", Locale.ROOT);

    /**
     * The save dialog's suggested name: the tab's name, when, and the type's
     * extensions, for example {@code people-202611281430.csv.zip}.
     */
    public static String suggestedName(String tab, LocalDateTime when, SaveType type) {
        return tab + "-" + STAMP.format(when) + type.extension();
    }

    /**
     * A type in the save dialog: a format, zipped or not. A zip holds the file
     * and the data credit, so zipped CSV is the default.
     */
    public record SaveType(ExportFormat format, boolean zip) {

        /** Every type, in the save dialog's order, the default first: each format zipped, then bare. */
        public static List<SaveType> all() {
            return java.util.Arrays.stream(values())
                    .flatMap(f -> java.util.stream.Stream.of(new SaveType(f, true), new SaveType(f, false)))
                    .toList();
        }

        /**
         * The extensions a name gets: the format's first, with {@code .zip}
         * after it when zipped. Zipped pipe is {@code .txt.zip}, as
         * faux-person-web names it, since {@code .txt} opens anywhere.
         */
        public String extension() {
            return zip ? "." + (format == PIPE ? "txt" : format.extensions.get(0)) + ZIP
                    : "." + format.extensions.get(0);
        }

        /** "CSV, zipped (*.csv.zip)" or "CSV (*.csv)", for the save dialog. */
        public String description() {
            List<String> shown = zip ? List.of("*" + extension())
                    : format.extensions.stream().map(x -> "*." + x).toList();
            return format.description + (zip ? ", zipped" : "") + " (" + String.join(", ", shown) + ")";
        }

        /**
         * The save dialog's patterns. A zipped type matches {@code *.zip}: a
         * save panel handles one extension more reliably than {@code .csv.zip}.
         */
        public List<String> patterns() {
            return zip ? List.of("*.zip") : format.extensions.stream().map(x -> "*." + x).toList();
        }

        /** "CSV, zipped" or "CSV", for the status bar. */
        public String label() {
            return format.description + (zip ? ", zipped" : "");
        }
    }

    /**
     * Where to save, and how.
     *
     * @param file   the file to write
     * @param format the rows' format
     * @param zip    whether {@code file} is a zip holding the rows as {@link #entryName()} and the data credit
     */
    public record Target(Path file, ExportFormat format, boolean zip) {

        /** A file that isn't zipped. */
        public Target(Path file, ExportFormat format) {
            this(file, format, false);
        }

        /** The rows' file name: inside the zip, or the file itself. */
        public String entryName() {
            String name = file.getFileName().toString();
            return zip ? name.substring(0, name.length() - ZIP.length()) : name;
        }

        /** "CSV, zipped" or "CSV", for the status bar. */
        public String label() {
            return new SaveType(format, zip).label();
        }
    }
}

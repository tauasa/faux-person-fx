package org.tauasa.faux.app;

import java.nio.file.Path;
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
     * The file to write and its format. A name whose extension names a format
     * is saved in that format. Otherwise it's saved as {@code fallback}, with
     * its default extension added.
     */
    public static Target resolve(Path chosen, ExportFormat fallback) {
        Optional<ExportFormat> named = forFileName(chosen);
        if (named.isPresent()) {
            return new Target(chosen, named.get());
        }
        Path withExtension = chosen.resolveSibling(chosen.getFileName() + "." + fallback.extensions.get(0));
        return new Target(withExtension, fallback);
    }

    /** Where to save, and how. */
    public record Target(Path file, ExportFormat format) {
    }
}

package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.tauasa.faux.app.ExportFormat.SaveType;

/** Which file a name and a chosen type give, zipped or not. Plain Java. */
class ExportFormatTest {

    private static final Path DIR = Path.of("out");
    private static final SaveType ZIPPED_CSV = new SaveType(ExportFormat.CSV, true);

    @ParameterizedTest(name = "{0} with {1} zipped={2} -> {3}, {4}, zipped={5}")
    @CsvSource({
        // A .zip name is zipped, and the name inside picks the format, or the type does.
        "names.csv.zip, CSV, false, names.csv.zip, CSV, true",
        "names.json.zip, CSV, true, names.json.zip, JSON, true",
        "names.TXT.ZIP, CSV, true, names.TXT.ZIP, PIPE, true",
        "names.zip, JSON, false, names.json.zip, JSON, true",
        // A name whose extension names a format is that format, bare, whatever the type.
        "names.csv, CSV, true, names.csv, CSV, false",
        "names.psv, JSON, true, names.psv, PIPE, false",
        // Any other name gets the type's extensions.
        "names, CSV, true, names.csv.zip, CSV, true",
        "names, JSON, false, names.json, JSON, false",
        "names.v2, PIPE, true, names.v2.txt.zip, PIPE, true",
        "names.psv.zip, CSV, true, names.psv.zip, PIPE, true",
    })
    void theNameAndTheTypeGiveTheFile(String chosen, ExportFormat format, boolean zip,
            String file, ExportFormat expectedFormat, boolean expectedZip) {
        ExportFormat.Target target = ExportFormat.resolve(DIR.resolve(chosen), new SaveType(format, zip));
        assertEquals(new ExportFormat.Target(DIR.resolve(file), expectedFormat, expectedZip), target);
    }

    @Test
    void aZipHoldsTheNameWithoutZip() {
        assertEquals("names.csv", ExportFormat.resolve(DIR.resolve("names"), ZIPPED_CSV).entryName());
        assertEquals("names.csv", ExportFormat.resolve(DIR.resolve("names.csv"), ZIPPED_CSV).entryName());
    }

    @Test
    void theSaveDialogListsEachFormatZippedThenBareWithZippedCsvFirst() {
        List<String> shown = SaveType.all().stream().map(SaveType::description).toList();
        assertEquals(List.of("CSV, zipped (*.csv.zip)", "CSV (*.csv)",
                "Pipe-delimited, zipped (*.txt.zip)", "Pipe-delimited (*.psv, *.txt)",
                "JSON, zipped (*.json.zip)", "JSON (*.json)"), shown);
        assertEquals(ZIPPED_CSV, SaveType.all().get(0), "the default");
        assertEquals(List.of("*.zip"), ZIPPED_CSV.patterns());
        assertEquals(List.of("*.psv", "*.txt"), new SaveType(ExportFormat.PIPE, false).patterns());
    }

    @Test
    void theSuggestedNameIsTheTabTheMinuteAndTheTypesExtensions() {
        LocalDateTime when = LocalDateTime.of(2026, 3, 7, 9, 5, 59);
        assertEquals("people-202603070905.csv.zip", ExportFormat.suggestedName("people", when, ZIPPED_CSV));
        assertEquals("people-202603070905.txt.zip",
                ExportFormat.suggestedName("people", when, new SaveType(ExportFormat.PIPE, true)));
        assertEquals("names-202603070905.json.zip",
                ExportFormat.suggestedName("names", when, new SaveType(ExportFormat.JSON, true)));
        assertEquals("companies-202603070905.psv",
                ExportFormat.suggestedName("companies", when, new SaveType(ExportFormat.PIPE, false)));
        assertEquals("people-202612312359.csv.zip",
                ExportFormat.suggestedName("people", LocalDateTime.of(2026, 12, 31, 23, 59), ZIPPED_CSV),
                "24-hour clock, zero-padded");
    }
}

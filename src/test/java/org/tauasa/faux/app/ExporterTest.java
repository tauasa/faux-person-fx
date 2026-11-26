package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.tauasa.faux.model.Name;
import org.tauasa.faux.model.Person;

class ExporterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private final FauxService service = new FauxService(new FauxFxProperties(100, 1000));

    private static final List<Name> TRICKY = List.of(
            new Name("Ana", "O'Neil, Jr."),
            new Name("Bo \"Bobby\"", "Pipe|Name"),
            new Name("Cy\nLine", "Back\\slash"));

    @Test
    void csvQuotesWhatRfc4180SaysAndEndsLinesWithCrlf() throws IOException {
        assertEquals("First name,Last name,Native name\r\n"
                + "Ana,\"O'Neil, Jr.\",\r\n"
                + "\"Bo \"\"Bobby\"\"\",Pipe|Name,\r\n"
                + "\"Cy\nLine\",Back\\slash,\r\n",
                write(Columns.names(), TRICKY, ExportFormat.CSV));
    }

    @Test
    void pipeQuotesFieldsHoldingAPipeAndEndsLinesWithLf() throws IOException {
        assertEquals("First name|Last name|Native name\n"
                + "Ana|O'Neil, Jr.|\n"
                + "\"Bo \"\"Bobby\"\"\"|\"Pipe|Name\"|\n"
                + "\"Cy\nLine\"|Back\\slash|\n",
                write(Columns.names(), TRICKY, ExportFormat.PIPE));
    }

    @Test
    void jsonParsesBackToTheSameValues() throws IOException {
        String json = write(Columns.names(), TRICKY, ExportFormat.JSON);
        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        assertEquals(3, rows.size());
        for (int i = 0; i < TRICKY.size(); i++) {
            assertEquals(TRICKY.get(i).firstName(), rows.get(i).get("firstName"));
            assertEquals(TRICKY.get(i).lastName(), rows.get(i).get("lastName"));
        }
    }

    @Test
    void jsonOfPeopleHasEveryColumnWithAgeAsANumberAndDatesAsIso() throws IOException {
        List<Person> people = service.people(new GenerateRequest(50, null, 4L, false)).rows();
        String json = write(Columns.people(TODAY), people, ExportFormat.JSON);
        List<Map<String, Object>> rows = JsonPath.read(json, "$");
        assertEquals(50, rows.size());
        Map<String, Object> first = rows.get(0);
        assertEquals(Columns.people(TODAY).size(), first.size());
        assertEquals(people.get(0).ageOn(TODAY), first.get("age"));
        assertEquals(people.get(0).dateOfBirth().toString(), first.get("dateOfBirth"));
        assertEquals(people.get(0).employer().domain(), first.get("employerDomain"));
        assertEquals(people.get(0).address().postalCode(), first.get("postalCode"));
    }

    /** A South Korean name's Hangul comes through every format unchanged. */
    @Test
    void nativeNamesExportInTheirOwnScript() throws IOException {
        List<Name> korean = List.of(new Name("Yeong-min", "Jeon", java.util.Optional.of(
                new org.tauasa.faux.model.NativeName("영민", "전", "전영민"))));
        assertEquals("First name,Last name,Native name\r\nYeong-min,Jeon,전영민\r\n",
                write(Columns.names(), korean, ExportFormat.CSV));
        assertEquals("First name|Last name|Native name\nYeong-min|Jeon|전영민\n",
                write(Columns.names(), korean, ExportFormat.PIPE));
        List<Map<String, Object>> rows = JsonPath.read(write(Columns.names(), korean, ExportFormat.JSON), "$");
        assertEquals("전영민", rows.get(0).get("nativeName"));
    }

    @Test
    void emptyTabWritesAHeaderOrAnEmptyArray() throws IOException {
        assertEquals("First name,Last name,Native name\r\n", write(Columns.names(), List.of(), ExportFormat.CSV));
        assertEquals(List.of(), JsonPath.read(write(Columns.names(), List.of(), ExportFormat.JSON), "$"));
    }

    @Test
    void generatedPeopleGiveOneLinePerRowPlusAHeader() throws IOException {
        List<Person> people = service.people(new GenerateRequest(200, null, 5L, false)).rows();
        String csv = write(Columns.people(TODAY), people, ExportFormat.CSV);
        assertEquals(201, csv.split("\r\n", -1).length - 1);
    }

    @Test
    void jsonKeysAreCamelCaseTitles() {
        assertEquals("dateOfBirth", Exporter.jsonKey("Date of birth"));
        assertEquals("lastName", Exporter.jsonKey("Last name"));
        assertEquals("employerDomain", Exporter.jsonKey("Employer domain"));
        assertEquals("age", Exporter.jsonKey("Age"));
    }

    @Test
    void jsonEscapesControlCharacters() {
        assertEquals("\"a\\u0001b\\tc\\\"d\\\\\"", Exporter.jsonString("a\u0001b\tc\"d\\"));
    }

    @Test
    void theExtensionPicksTheFormatElseTheChosenTypeAddsOne() {
        assertEquals(new ExportFormat.Target(Path.of("a.json"), ExportFormat.JSON),
                ExportFormat.resolve(Path.of("a.json"), ExportFormat.CSV));
        assertEquals(new ExportFormat.Target(Path.of("a.TXT"), ExportFormat.PIPE),
                ExportFormat.resolve(Path.of("a.TXT"), ExportFormat.CSV));
        assertEquals(new ExportFormat.Target(Path.of("dir/people.psv"), ExportFormat.PIPE),
                ExportFormat.resolve(Path.of("dir/people"), ExportFormat.PIPE));
        assertEquals(new ExportFormat.Target(Path.of("people.v2.csv"), ExportFormat.CSV),
                ExportFormat.resolve(Path.of("people.v2"), ExportFormat.CSV));
        assertTrue(ExportFormat.forFileName(Path.of("people")).isEmpty());
    }

    private static <T> String write(List<Column<T>> columns, List<T> rows, ExportFormat format) throws IOException {
        StringWriter out = new StringWriter();
        Exporter.write(columns, rows, format, out);
        return out.toString();
    }
}

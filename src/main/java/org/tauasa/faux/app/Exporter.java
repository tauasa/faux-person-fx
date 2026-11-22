package org.tauasa.faux.app;

import java.io.IOException;
import java.io.Writer;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;

/**
 * Writes rows as CSV, pipe-delimited text or JSON, one field per column, in
 * the order given.
 *
 * <ul>
 * <li><b>CSV</b>: RFC 4180. A header row of column titles, CRLF line ends, and
 * a field in double quotes when it holds a comma, a quote, CR or LF, with its
 * quotes doubled.</li>
 * <li><b>Pipe</b>: the same, with {@code |} for the comma and LF line ends.</li>
 * <li><b>JSON</b>: an array of objects, one per row, keyed by the column title
 * in camelCase ({@code "Date of birth"} is {@code dateOfBirth}). Numbers are
 * numbers, dates ISO-8601 strings, everything else a string.</li>
 * </ul>
 */
public final class Exporter {

    private Exporter() {
    }

    public static <T> void write(List<Column<T>> columns, List<T> rows, ExportFormat format, Writer out)
            throws IOException {
        switch (format) {
            case CSV -> delimited(columns, rows, ',', "\r\n", out);
            case PIPE -> delimited(columns, rows, '|', "\n", out);
            case JSON -> json(columns, rows, out);
        }
        out.flush();
    }

    /** "Date of birth" becomes "dateOfBirth". */
    public static String jsonKey(String title) {
        String[] words = title.trim().split("[^\\p{Alnum}]+");
        StringBuilder key = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            String lower = word.toLowerCase(Locale.ROOT);
            key.append(key.length() == 0 ? lower
                    : Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return key.toString();
    }

    private static <T> void delimited(List<Column<T>> columns, List<T> rows, char delimiter, String lineEnd,
            Writer out) throws IOException {
        line(columns.stream().map(Column::title).toList(), delimiter, lineEnd, out);
        for (T row : rows) {
            line(columns.stream().map(c -> String.valueOf(c.valueOf(row))).toList(), delimiter, lineEnd, out);
        }
    }

    private static void line(List<String> fields, char delimiter, String lineEnd, Writer out) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                out.write(delimiter);
            }
            out.write(field(fields.get(i), delimiter));
        }
        out.write(lineEnd);
    }

    static String field(String value, char delimiter) {
        boolean quote = value.indexOf(delimiter) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
        return quote ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }

    private static <T> void json(List<Column<T>> columns, List<T> rows, Writer out) throws IOException {
        List<String> keys = columns.stream().map(c -> jsonKey(c.title())).toList();
        out.write('[');
        for (int r = 0; r < rows.size(); r++) {
            out.write(r == 0 ? "\n  {" : ",\n  {");
            T row = rows.get(r);
            for (int c = 0; c < columns.size(); c++) {
                out.write(c == 0 ? "\n    " : ",\n    ");
                out.write(jsonString(keys.get(c)));
                out.write(": ");
                out.write(jsonValue(columns.get(c).valueOf(row)));
            }
            out.write("\n  }");
        }
        out.write(rows.isEmpty() ? "]\n" : "\n]\n");
    }

    private static String jsonValue(Object value) {
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof TemporalAccessor) {
            return jsonString(value.toString());
        }
        return jsonString(String.valueOf(value));
    }

    static String jsonString(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}

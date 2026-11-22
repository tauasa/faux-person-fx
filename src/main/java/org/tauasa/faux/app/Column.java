package org.tauasa.faux.app;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

/**
 * One table column: its title and the value it shows for a row. The value is
 * {@link Comparable}, so a date sorts as a date and an age as a number.
 *
 * @param title the column header
 * @param value the cell's value for a row, never {@code null}
 * @param <T>   the row type
 */
public record Column<T>(String title, Function<T, ? extends Comparable<?>> value) {

    /**
     * The order every column sorts its values in. Text sorts ignoring case,
     * then by its exact characters, so equal-looking values keep a fixed order.
     * Anything else sorts by its natural order.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final Comparator<Object> VALUE_ORDER = (a, b) -> {
        if (a instanceof String s && b instanceof String t) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(s, t);
            return c != 0 ? c : s.compareTo(t);
        }
        return ((Comparable) a).compareTo(b);
    };

    public Column {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(value, "value");
    }

    /** This column's value for a row. */
    public Object valueOf(T row) {
        return value.apply(row);
    }

    /** Rows in this column's ascending order. */
    public Comparator<T> comparator() {
        return (a, b) -> VALUE_ORDER.compare(valueOf(a), valueOf(b));
    }
}

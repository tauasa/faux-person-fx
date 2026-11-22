package org.tauasa.faux.app;

import java.util.List;

/**
 * Rows a generator made, and the seed that replays them.
 *
 * @param rows the rows, in the order generated
 * @param seed the seed used, random or given
 * @param <T>  the row type
 */
public record Generated<T>(List<T> rows, long seed) {

    public Generated {
        rows = List.copyOf(rows);
    }
}

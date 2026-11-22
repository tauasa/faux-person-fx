package org.tauasa.faux.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.tauasa.faux.model.Region;

/**
 * The runs one tab has made, most recent first, so a sample can be made
 * again. One entry per seed: running a seed again moves it to the top with
 * the settings it ran with last. Holds at most {@link #LIMIT}.
 */
final class SeedHistory {

    static final int LIMIT = 20;

    /**
     * One Generate: the seed and the settings that, with it, make the same rows.
     *
     * @param seed          the seed used
     * @param count         how many rows
     * @param region        the region, or {@code null} for mixed
     * @param realCompanies whether employers were real
     */
    record Run(long seed, int count, Region region, boolean realCompanies) {

        static Run of(GenerateRequest request, long seed) {
            return new Run(seed, request.count(), request.region(), request.realCompanies());
        }

        /** "25 names, Mixed", "10 people, JPN, real companies", "8 companies, real". */
        String describe(Kind kind) {
            StringBuilder text = new StringBuilder(kind.count(count));
            if (kind != Kind.COMPANIES) {
                text.append(", ").append(region == null ? MainView.MIXED : region.name());
            }
            if (kind != Kind.NAMES && realCompanies) {
                text.append(kind == Kind.COMPANIES ? ", real" : ", real companies");
            }
            return text.toString();
        }
    }

    private final List<Run> runs = new ArrayList<>();

    /** Puts back runs loaded from disk, newest first, in place of any held now. */
    void restore(List<Run> newestFirst) {
        runs.clear();
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            add(newestFirst.get(i));
        }
    }

    void add(Run run) {
        runs.removeIf(r -> r.seed() == run.seed());
        runs.add(0, run);
        while (runs.size() > LIMIT) {
            runs.remove(runs.size() - 1);
        }
    }

    /** Most recent first. */
    List<Run> runs() {
        return List.copyOf(runs);
    }

    /** The last run, if any. */
    Optional<Run> last() {
        return runs.isEmpty() ? Optional.empty() : Optional.of(runs.get(0));
    }

    /** The run a seed, as text, names. */
    Optional<Run> find(String seedText) {
        Long seed;
        try {
            seed = GenerateRequest.parseSeed(seedText);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (seed == null) {
            return Optional.empty();
        }
        return runs.stream().filter(r -> r.seed() == seed).findFirst();
    }
}

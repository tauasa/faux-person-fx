package org.tauasa.faux.app;

import org.tauasa.faux.model.Region;

/**
 * What to generate.
 *
 * @param count         how many rows, 1 or more
 * @param region        one region, or {@code null} for mixed. Companies ignore it
 * @param seed          a seed to replay, or {@code null} for a random one
 * @param realCompanies real employer names and domains instead of {@code [faux]} ones under {@code .example}.
 *                      Names ignore it
 */
public record GenerateRequest(int count, Region region, Long seed, boolean realCompanies) {

    public GenerateRequest {
        if (count < 1) {
            throw new IllegalArgumentException("count must be 1 or more: " + count);
        }
    }

    /**
     * Reads a seed as typed in the window.
     *
     * @param text what was typed
     * @return the seed, or {@code null} when the text is blank
     * @throws IllegalArgumentException if the text isn't a whole number that fits in a {@code long}
     */
    public static Long parseSeed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Seed must be a whole number from " + Long.MIN_VALUE + " to " + Long.MAX_VALUE, e);
        }
    }
}

package org.tauasa.faux.app;

import java.nio.file.Path;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code faux.fx} in {@code application.properties}.
 *
 * @param defaultCount   the count the window opens with
 * @param maxCount       the most rows one Generate may make
 * @param historyFile    where each tab's seed history is kept between sessions. A leading {@code ~/} means
 *                       the home folder. Blank keeps no history on disk
 * @param appearanceFile where the Help &gt; Appearance choice is kept between sessions, the same way. Blank
 *                       keeps none, so each session starts in System
 */
@ConfigurationProperties("faux.fx")
public record FauxFxProperties(int defaultCount, int maxCount, String historyFile, String appearanceFile) {

    @ConstructorBinding
    public FauxFxProperties(@DefaultValue("100") int defaultCount, @DefaultValue("10000") int maxCount,
            @DefaultValue("") String historyFile, @DefaultValue("") String appearanceFile) {
        if (maxCount < 1) {
            throw new IllegalArgumentException("faux.fx.max-count must be 1 or more: " + maxCount);
        }
        if (defaultCount < 1 || defaultCount > maxCount) {
            throw new IllegalArgumentException(
                    "faux.fx.default-count must be from 1 to faux.fx.max-count (" + maxCount + "): " + defaultCount);
        }
        this.defaultCount = defaultCount;
        this.maxCount = maxCount;
        this.historyFile = historyFile == null ? "" : historyFile.strip();
        this.appearanceFile = appearanceFile == null ? "" : appearanceFile.strip();
    }

    /** Settings that keep no seed history and no appearance on disk. */
    public FauxFxProperties(int defaultCount, int maxCount) {
        this(defaultCount, maxCount, "", "");
    }

    /** Settings that keep seed history in {@code historyFile} and no appearance on disk. */
    public FauxFxProperties(int defaultCount, int maxCount, String historyFile) {
        this(defaultCount, maxCount, historyFile, "");
    }

    /** The history file, or empty when history isn't kept on disk. */
    public Optional<Path> historyPath() {
        return expand(historyFile);
    }

    /** The appearance file, or empty when the appearance isn't kept on disk. */
    public Optional<Path> appearancePath() {
        return expand(appearanceFile);
    }

    /** A path setting: blank is none, and a leading {@code ~/} is the home folder. */
    static Optional<Path> expand(String setting) {
        String text = setting == null ? "" : setting.strip();
        if (text.isEmpty()) {
            return Optional.empty();
        }
        if (text.equals("~") || text.startsWith("~/")) {
            return Optional.of(Path.of(System.getProperty("user.home"), text.substring(1)).normalize());
        }
        return Optional.of(Path.of(text));
    }
}

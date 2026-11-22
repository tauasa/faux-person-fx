package org.tauasa.faux.app;

import java.nio.file.Path;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings under {@code faux.fx} in {@code application.properties}.
 *
 * @param defaultCount the count the window opens with
 * @param maxCount     the most rows one Generate may make
 * @param historyFile  where each tab's seed history is kept between sessions. A leading {@code ~/} means
 *                     the home folder. Blank keeps no history on disk
 */
@ConfigurationProperties("faux.fx")
public record FauxFxProperties(int defaultCount, int maxCount, String historyFile) {

    @ConstructorBinding
    public FauxFxProperties(@DefaultValue("100") int defaultCount, @DefaultValue("10000") int maxCount,
            @DefaultValue("") String historyFile) {
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
    }

    /** Settings that keep no seed history on disk. */
    public FauxFxProperties(int defaultCount, int maxCount) {
        this(defaultCount, maxCount, "");
    }

    /** The history file, or empty when history isn't kept on disk. */
    public Optional<Path> historyPath() {
        if (historyFile.isEmpty()) {
            return Optional.empty();
        }
        if (historyFile.equals("~") || historyFile.startsWith("~/")) {
            return Optional.of(Path.of(System.getProperty("user.home"), historyFile.substring(1)).normalize());
        }
        return Optional.of(Path.of(historyFile));
    }
}

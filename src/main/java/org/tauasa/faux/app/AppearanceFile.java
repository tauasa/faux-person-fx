package org.tauasa.faux.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remembers the Help > Appearance choice between sessions: one word,
 * {@code LIGHT}, {@code DARK} or {@code SYSTEM}, in a text file. A file that's
 * missing or unreadable means {@link Appearance#SYSTEM}; a failure to write is
 * logged and otherwise ignored.
 */
final class AppearanceFile {

    /** Where it's kept unless {@code faux.fx.appearance-file} says otherwise. */
    static final String DEFAULT = "~/.faux-person-fx/appearance.txt";

    private static final Logger LOG = LoggerFactory.getLogger(AppearanceFile.class);

    private final Path file;

    AppearanceFile(Path file) {
        this.file = file;
    }

    Appearance load() {
        try {
            return Appearance.parse(Files.readString(file, StandardCharsets.UTF_8)).orElse(Appearance.SYSTEM);
        } catch (NoSuchFileException e) {
            return Appearance.SYSTEM;
        } catch (IOException e) {
            LOG.warn("Couldn't read the appearance in {}: {}", file, e.toString());
            return Appearance.SYSTEM;
        }
    }

    void save(Appearance appearance) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, appearance.name() + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Couldn't save the appearance to {}: {}", file, e.toString());
        }
    }

    /**
     * The appearance the splash screen should start in, read before Spring
     * runs: from {@code -Dfaux.fx.appearance-file} if given, else the default
     * file. Blank means none is kept, so {@link Appearance#SYSTEM}.
     */
    static Appearance forSplash() {
        return FauxFxProperties.expand(System.getProperty("faux.fx.appearance-file", DEFAULT))
                .map(path -> new AppearanceFile(path).load())
                .orElse(Appearance.SYSTEM);
    }
}

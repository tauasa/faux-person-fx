package org.tauasa.faux.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tauasa.faux.model.Region;

/**
 * Keeps each tab's {@link SeedHistory} in a text file between sessions. One
 * run per line, newest first within each tab:
 *
 * <pre>
 * NAMES,42,25,MIXED,false
 * PEOPLE,-7,100,JPN,true
 * </pre>
 *
 * A line it can't read, such as one naming a region faux-person no longer has,
 * is skipped, and the rest are kept. A file that can't be read or written is
 * logged and otherwise ignored: the app never fails over its history.
 */
final class SeedHistoryFile {

    private static final Logger LOG = LoggerFactory.getLogger(SeedHistoryFile.class);
    private static final String MIXED = "MIXED";
    private static final String HEADER = """
            # faux-person-fx seed history, written by the app. Safe to delete.
            # One run per line, newest first within each tab: tab,seed,count,region,realCompanies
            """;

    private final Path file;

    SeedHistoryFile(Path file) {
        this.file = file;
    }

    Path file() {
        return file;
    }

    /** Each tab's runs, newest first. Tabs with none are left out. */
    Map<Kind, List<SeedHistory.Run>> load() {
        Map<Kind, List<SeedHistory.Run>> runs = new EnumMap<>(Kind.class);
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            return runs;
        } catch (IOException e) {
            LOG.warn("Couldn't read the seed history in {}: {}", file, e.toString());
            return runs;
        }
        int skipped = 0;
        for (String line : lines) {
            String text = line.strip();
            if (text.isEmpty() || text.startsWith("#")) {
                continue;
            }
            try {
                String[] f = text.split(",", -1);
                if (f.length != 5) {
                    throw new IllegalArgumentException("expected 5 fields");
                }
                Kind kind = Kind.valueOf(f[0]);
                int count = Integer.parseInt(f[2]);
                if (count < 1) {
                    throw new IllegalArgumentException("count below 1");
                }
                Region region = f[3].equals(MIXED) ? null : Region.valueOf(f[3]);
                if (!f[4].equals("true") && !f[4].equals("false")) {
                    throw new IllegalArgumentException("realCompanies isn't true or false");
                }
                List<SeedHistory.Run> list = runs.computeIfAbsent(kind, k -> new ArrayList<>());
                if (list.size() < SeedHistory.LIMIT) {
                    list.add(new SeedHistory.Run(Long.parseLong(f[1]), count, region, Boolean.parseBoolean(f[4])));
                }
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }
        if (skipped > 0) {
            LOG.warn("Skipped {} unreadable line(s) in the seed history {}", skipped, file);
        }
        return runs;
    }

    /**
     * Replaces the file with these histories, all at once: it's written beside
     * the file and moved over it, so a crash mid-write leaves the old one.
     */
    void save(Map<Kind, SeedHistory> histories) {
        StringBuilder out = new StringBuilder(HEADER);
        for (Map.Entry<Kind, SeedHistory> entry : histories.entrySet()) {
            for (SeedHistory.Run run : entry.getValue().runs()) {
                out.append(entry.getKey().name()).append(',')
                        .append(run.seed()).append(',')
                        .append(run.count()).append(',')
                        .append(run.region() == null ? MIXED : run.region().name()).append(',')
                        .append(run.realCompanies()).append('\n');
            }
        }
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, ".seed-history", ".tmp");
            try {
                Files.writeString(temp, out, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOG.warn("Couldn't save the seed history to {}: {}", file, e.toString());
        }
    }
}

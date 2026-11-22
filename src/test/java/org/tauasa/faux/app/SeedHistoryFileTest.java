package org.tauasa.faux.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tauasa.faux.model.Region;

class SeedHistoryFileTest {

    @TempDir
    Path dir;

    @Test
    void savedHistoriesLoadBackTheSameNewestFirst() {
        SeedHistory names = new SeedHistory();
        names.add(new SeedHistory.Run(1, 10, null, false));
        names.add(new SeedHistory.Run(-2, 25, Region.JPN, false));
        SeedHistory people = new SeedHistory();
        people.add(new SeedHistory.Run(Long.MAX_VALUE, 500, Region.BRA, true));
        Map<Kind, SeedHistory> histories = new EnumMap<>(Kind.class);
        histories.put(Kind.NAMES, names);
        histories.put(Kind.COMPANIES, new SeedHistory());
        histories.put(Kind.PEOPLE, people);

        SeedHistoryFile file = new SeedHistoryFile(dir.resolve("nested/folder/history.txt"));
        file.save(histories);
        Map<Kind, List<SeedHistory.Run>> loaded = file.load();

        assertEquals(names.runs(), loaded.get(Kind.NAMES));
        assertEquals(people.runs(), loaded.get(Kind.PEOPLE));
        assertTrue(!loaded.containsKey(Kind.COMPANIES), "an empty tab came back with runs");
    }

    @Test
    void aMissingFileIsAnEmptyHistory() {
        assertTrue(new SeedHistoryFile(dir.resolve("none.txt")).load().isEmpty());
    }

    @Test
    void unreadableLinesAreSkippedAndTheRestKept() throws IOException {
        Path path = dir.resolve("history.txt");
        Files.writeString(path, """
                # a comment
                NAMES,5,10,MIXED,false
                NAMES,not-a-seed,10,MIXED,false
                NAMES,6,10,ATLANTIS,false
                NAMES,7,0,MIXED,false
                NAMES,8,10,MIXED,maybe
                PLANETS,9,10,MIXED,false
                NAMES,10,10,MIXED
                PEOPLE,11,3,JPN,true

                """);
        Map<Kind, List<SeedHistory.Run>> loaded = new SeedHistoryFile(path).load();
        assertEquals(List.of(new SeedHistory.Run(5, 10, null, false)), loaded.get(Kind.NAMES));
        assertEquals(List.of(new SeedHistory.Run(11, 3, Region.JPN, true)), loaded.get(Kind.PEOPLE));
    }

    @Test
    void aLongFileLoadsOnlyTheLimitPerTab() throws IOException {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < SeedHistory.LIMIT + 10; i++) {
            text.append("NAMES,").append(i).append(",1,MIXED,false\n");
        }
        Path path = dir.resolve("history.txt");
        Files.writeString(path, text);
        List<SeedHistory.Run> names = new SeedHistoryFile(path).load().get(Kind.NAMES);
        assertEquals(SeedHistory.LIMIT, names.size());
        assertEquals(0L, names.get(0).seed(), "the newest, first in the file, was dropped");
    }

    @Test
    void savingReplacesTheFileAndLeavesNoTempFileBehind() throws IOException {
        Path path = dir.resolve("history.txt");
        Files.writeString(path, "NAMES,99,1,MIXED,false\n");
        SeedHistory names = new SeedHistory();
        names.add(new SeedHistory.Run(1, 1, null, false));
        new SeedHistoryFile(path).save(Map.of(Kind.NAMES, names));

        assertEquals(List.of(new SeedHistory.Run(1, 1, null, false)),
                new SeedHistoryFile(path).load().get(Kind.NAMES));
        try (var files = Files.list(dir)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void aFileThatCantBeWrittenDoesntThrow() throws IOException {
        Path blocker = dir.resolve("a-file");
        Files.writeString(blocker, "x");
        // Its parent is a file, so the folder can't be made.
        new SeedHistoryFile(blocker.resolve("history.txt")).save(Map.of(Kind.NAMES, new SeedHistory()));
        assertTrue(Files.isRegularFile(blocker));
    }

    @Test
    void restorePutsRunsBackNewestFirst() {
        SeedHistory history = new SeedHistory();
        history.add(new SeedHistory.Run(100, 1, null, false));
        history.restore(List.of(new SeedHistory.Run(3, 1, null, false), new SeedHistory.Run(2, 1, null, false)));
        assertEquals(List.of(3L, 2L), history.runs().stream().map(SeedHistory.Run::seed).toList());
    }

    @Test
    void theHistoryFileSettingExpandsTheHomeFolderAndBlankTurnsItOff() {
        String home = System.getProperty("user.home");
        assertEquals(Path.of(home, ".faux-person-fx", "h.txt"),
                new FauxFxProperties(1, 1, "~/.faux-person-fx/h.txt").historyPath().orElseThrow());
        assertEquals(Path.of("/tmp/h.txt"), new FauxFxProperties(1, 1, "/tmp/h.txt").historyPath().orElseThrow());
        assertTrue(new FauxFxProperties(1, 1, "  ").historyPath().isEmpty());
        assertTrue(new FauxFxProperties(1, 1).historyPath().isEmpty());
    }
}

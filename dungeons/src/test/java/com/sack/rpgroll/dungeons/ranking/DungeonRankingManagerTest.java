package com.sack.rpgroll.dungeons.ranking;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DungeonRankingManagerTest {

    @TempDir
    Path tempDir;

    private DungeonRankingManager manager;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        manager = new DungeonRankingManager(plugin);
    }

    private DungeonRunResult result(String difficulty, long durationMillis, int deaths, double damage) {
        return new DungeonRunResult("crypt", difficulty, List.of("Steve"), System.currentTimeMillis(),
                durationMillis, deaths, damage);
    }

    @Test
    void topReturnsEntriesSortedByScoreDescending() {
        manager.recordRun(result("normal", 60_000, 1, 100));
        manager.recordRun(result("normal", 30_000, 0, 50));

        List<DungeonRunResult> top = manager.top("crypt", RankingPeriod.GLOBAL, 10);

        assertEquals(2, top.size());
        assertTrue(top.get(0).score() >= top.get(1).score());
    }

    @Test
    void topRespectsLimit() {
        manager.recordRun(result("normal", 60_000, 0, 0));
        manager.recordRun(result("normal", 50_000, 0, 0));
        manager.recordRun(result("normal", 40_000, 0, 0));

        assertEquals(1, manager.top("crypt", RankingPeriod.GLOBAL, 1).size());
    }

    @Test
    void topForUnknownDungeonIsEmpty() {
        assertTrue(manager.top("unknown-dungeon", RankingPeriod.GLOBAL, 10).isEmpty());
    }

    @Test
    void dailyPeriodExcludesOldEntries() {
        DungeonRunResult oldRun = new DungeonRunResult("crypt", "normal", List.of("Steve"),
                System.currentTimeMillis() - (2L * RankingPeriod.DAILY.windowMillis()), 60_000, 0, 0);
        manager.recordRun(oldRun);

        assertTrue(manager.top("crypt", RankingPeriod.DAILY, 10).isEmpty());
        assertEquals(1, manager.top("crypt", RankingPeriod.GLOBAL, 10).size());
    }

    @Test
    void recordRunPersistsAcrossManagerInstancesViaYamlFile() {
        manager.recordRun(result("normal", 60_000, 0, 0));

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test2"));
        DungeonRankingManager reloaded = new DungeonRankingManager(plugin);

        assertEquals(1, reloaded.top("crypt", RankingPeriod.GLOBAL, 10).size());
    }
}

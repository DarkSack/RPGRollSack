package com.sack.rpgroll.structures.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceSettingsTest {

    private static YamlConfiguration yaml(String s) {
        try {
            YamlConfiguration y = new YamlConfiguration();
            y.loadFromString(s);
            return y;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void defaultsMatchThePlan() {
        var s = PerformanceSettings.from(yaml(""), w -> fail(w));
        assertEquals(new PerformanceSettings(256, 10, 5, 2, 4096, 64, 300, 100, 60), s);
    }

    @Test
    void readsValues() {
        var s = PerformanceSettings.from(yaml("blocks-per-tick: 100\nmax-projects:\n  server: 3\n  world: 2\n  player: 1\n"
                + "preview:\n  max-blocks: 10\n  max-distance: 20\n  seconds: 30\ncancel-refund: 50\n"
                + "protection-recheck-seconds: 15\n"), w -> fail(w));
        assertEquals(new PerformanceSettings(100, 3, 2, 1, 10, 20, 30, 50, 15), s);
    }

    @Test
    void badValueWarnsWithKeyAndFallsBack() {
        List<String> warns = new ArrayList<>();
        var s = PerformanceSettings.from(yaml("blocks-per-tick: 0\ncancel-refund: 101\n"), warns::add);
        assertEquals(256, s.blocksPerTick());
        assertEquals(100, s.cancelRefundPercent());
        assertEquals(2, warns.size());
        assertTrue(warns.get(0).contains("blocks-per-tick"));
        assertTrue(warns.get(1).contains("cancel-refund"));
    }

    @Test
    void zeroRefundIsValid() {
        assertEquals(0, PerformanceSettings.from(yaml("cancel-refund: 0"), w -> fail(w)).cancelRefundPercent());
    }

    @Test
    void protectionRecheckDefaultsTo60() {
        assertEquals(60, PerformanceSettings.from(yaml(""), w -> fail(w)).protectionRecheckSeconds());
        List<String> warns = new ArrayList<>();
        assertEquals(60, PerformanceSettings.from(yaml("protection-recheck-seconds: 0"), warns::add)
                .protectionRecheckSeconds());
        assertTrue(warns.getFirst().contains("protection-recheck-seconds"));
    }
}

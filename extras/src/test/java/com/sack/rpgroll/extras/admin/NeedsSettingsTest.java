package com.sack.rpgroll.extras.admin;

import com.sack.rpgroll.extras.stat.StatDefinition;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeedsSettingsTest {

    @Test
    void speedCyclesThroughTheFixedSteps() {
        assertEquals(1.5, NeedsSettings.nextSpeed(1.0));
        assertEquals(3.0, NeedsSettings.nextSpeed(2.0));
        assertEquals(0.25, NeedsSettings.nextSpeed(3.0));
        // Un valor escrito a mano en el YAML salta al siguiente paso.
        assertEquals(1.0, NeedsSettings.nextSpeed(0.7));
    }

    @Test
    void toggleAddsOrRemovesAWorld() {
        assertEquals(List.of("lobby", "world"), NeedsSettings.toggle(List.of("lobby"), "world"));
        assertEquals(List.of(), NeedsSettings.toggle(List.of("lobby"), "lobby"));
    }

    @Test
    void idleTimeNeverGoesUnderOneMinute() {
        assertEquals(360, NeedsSettings.idleStep(300, 60));
        assertEquals(60, NeedsSettings.idleStep(60, -60));
    }

    @Test
    void curingFillsNeedsThatDrainAndEmptiesOnesThatBuildUp() {
        StatDefinition thirst = new StatDefinition("thirst", true, 100, 100, null, null, Map.of(), List.of());
        StatDefinition stress = new StatDefinition("stress", true, 100, 0, null, null, Map.of(), List.of());
        assertEquals(100.0, NeedsSettings.cureValue(thirst));
        assertEquals(0.0, NeedsSettings.cureValue(stress));
    }

    @Test
    void findsTheStatFileByNameOrById(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("thirst.yml"), "id: thirst\n");
        java.nio.file.Files.createDirectories(dir.resolve("extra"));
        java.nio.file.Files.writeString(dir.resolve("extra/ansiedad.yml"), "id: stress\n");

        assertEquals(dir.resolve("thirst.yml").toFile(), NeedsSettings.findStatFile(dir.toFile(), "thirst"));
        assertEquals(dir.resolve("extra/ansiedad.yml").toFile(), NeedsSettings.findStatFile(dir.toFile(), "stress"));
        assertEquals(null, NeedsSettings.findStatFile(dir.toFile(), "mana"));
    }

    @Test
    void writingAKeyKeepsTheRestOfTheFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file = dir.resolve("thirst.yml");
        java.nio.file.Files.writeString(file, "# Sed\nid: thirst\nenabled: true\nmax: 100\n");

        NeedsSettings.write(file.toFile(), "enabled", false);

        String text = java.nio.file.Files.readString(file);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("enabled: false"), text);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("max: 100"), text);
        org.junit.jupiter.api.Assertions.assertTrue(text.contains("# Sed"), text);
    }
}

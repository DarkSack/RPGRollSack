package com.sack.rpgroll.dungeons.structure;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureParserTest {

    private final StructureParser parser = new StructureParser();

    private YamlConfiguration load(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void missingIdThrows() {
        YamlConfiguration config = load("source: CUSTOM");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void nativeSourceSkipsPaletteAndLayersEntirely() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: NATIVE
                """);

        StructureDefinition def = parser.parse(config);

        assertEquals(StructureSourceType.NATIVE, def.sourceType());
        assertEquals(0, def.width());
        assertTrue(def.palette().isEmpty());
        assertTrue(def.layers().isEmpty());
    }

    @Test
    void unknownSourceFallsBackToCustom() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: NOT_A_SOURCE
                """);

        assertEquals(StructureSourceType.CUSTOM, parser.parse(config).sourceType());
    }

    @Test
    void customSourceDerivesDimensionsFromLayers() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: CUSTOM
                palette:
                  S: STONE
                layers:
                  - ["SS", "SS"]
                  - ["SS", "SS"]
                """);

        StructureDefinition def = parser.parse(config);

        assertEquals(2, def.height());
        assertEquals(2, def.depth());
        assertEquals(2, def.width());
        assertEquals(Material.STONE, def.palette().get('S'));
        assertEquals(Material.AIR, def.palette().get('.'));
        assertEquals(Material.AIR, def.palette().get(' '));
    }

    @Test
    void emptyLayersProduceZeroDimensions() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: CUSTOM
                """);

        StructureDefinition def = parser.parse(config);

        assertEquals(0, def.height());
        assertEquals(0, def.depth());
        assertEquals(0, def.width());
    }

    @Test
    void invalidPaletteMaterialIsSkipped() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: CUSTOM
                palette:
                  X: NOT_A_MATERIAL
                """);

        StructureDefinition def = parser.parse(config);

        assertEquals(2, def.palette().size());
    }

    @Test
    void parsesAnchorOffsets() {
        YamlConfiguration config = load("""
                id: crypt-room
                source: CUSTOM
                anchor:
                  x: 1
                  y: 2
                  z: 3
                """);

        StructureDefinition def = parser.parse(config);

        assertEquals(1, def.anchorX());
        assertEquals(2, def.anchorY());
        assertEquals(3, def.anchorZ());
    }
}

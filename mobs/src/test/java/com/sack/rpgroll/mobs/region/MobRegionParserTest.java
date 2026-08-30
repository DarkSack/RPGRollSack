package com.sack.rpgroll.mobs.region;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MobRegionParserTest {

    private final MobRegionParser parser = new MobRegionParser();

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
        YamlConfiguration config = load("world: world");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void worldDefaultsWhenMissing() {
        YamlConfiguration config = load("""
                id: arena
                min:
                  x: 0
                  y: 0
                  z: 0
                max:
                  x: 10
                  y: 10
                  z: 10
                """);

        MobRegion region = parser.parse(config);

        assertEquals("world", region.world());
        assertEquals(0.0, region.minX());
        assertEquals(10.0, region.maxX());
    }

    @Test
    void parsesExplicitBounds() {
        YamlConfiguration config = load("""
                id: arena
                world: arena_world
                min:
                  x: -5
                  y: 60
                  z: -5
                max:
                  x: 5
                  y: 100
                  z: 5
                """);

        MobRegion region = parser.parse(config);

        assertEquals("arena_world", region.world());
        assertEquals(-5.0, region.minX());
        assertEquals(100.0, region.maxY());
    }
}

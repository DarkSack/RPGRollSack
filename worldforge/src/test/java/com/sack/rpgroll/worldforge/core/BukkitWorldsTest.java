package com.sack.rpgroll.worldforge.core;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BukkitWorldsTest {

    private static Map<String, String> read(String yaml) throws InvalidConfigurationException {
        YamlConfiguration c = new YamlConfiguration();
        c.loadFromString(yaml);
        return BukkitWorlds.using(c, "RPGRoll-WorldForge");
    }

    @Test
    void only_the_worlds_that_use_worldforge() throws InvalidConfigurationException {
        assertEquals(Map.of("world", "void-platform", "islas", ""), read("""
                settings: {allow-end: true}
                worlds:
                  world:
                    generator: RPGRoll-WorldForge:void-platform
                  otro:
                    generator: Terra:OVERWORLD
                  islas:
                    generator: rpgroll-worldforge
                  sin:
                    environment: NORMAL
                """));
        assertEquals(Map.of(), read("settings: {allow-end: true}\n"));
        assertEquals(Map.of(), read("worlds:\n  world:\n    generator: ''\n"));
    }
}

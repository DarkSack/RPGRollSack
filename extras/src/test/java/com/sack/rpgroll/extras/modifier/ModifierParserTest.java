package com.sack.rpgroll.extras.modifier;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierParserTest {

    private final ModifierParser parser = new ModifierParser();

    @Test
    void parsesTypeAndLowercasesValueKeys() throws Exception {
        YamlConfiguration config = load("""
                id: elf
                type: RACE
                values:
                  Cold_Resistance: 0.2
                  STAT_MAX: -0.1
                """);

        ModifierSet set = parser.parse(config);

        assertEquals("elf", set.id());
        assertEquals(ModifierSourceType.RACE, set.type());
        assertEquals(0.2, set.values().get("cold_resistance"));
        assertEquals(-0.1, set.values().get("stat_max"));
    }

    @Test
    void defaultsToRaceTypeWhenOmitted() throws Exception {
        ModifierSet set = parser.parse(load("id: elf"));

        assertEquals(ModifierSourceType.RACE, set.type());
        assertTrue(set.values().isEmpty());
    }

    @Test
    void missingIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("type: JOB")));
    }

    @Test
    void invalidTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
                id: elf
                type: NOT_A_TYPE
                """)));
    }

    private YamlConfiguration load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}

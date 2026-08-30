package com.sack.rpgroll.items.socket;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemParserTest {

    private final GemParser parser = new GemParser();

    private YamlConfiguration configFrom(String yaml) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return config;
    }

    @Test
    void parseThrowsWhenIdMissing() {
        YamlConfiguration config = configFrom("display-name: Ruby");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void parseDefaultsDisplayNameAndTypeWhenAbsent() {
        YamlConfiguration config = configFrom("id: ruby");

        Gem gem = parser.parse(config);

        assertEquals("ruby", gem.displayName());
        assertEquals("GENERIC", gem.type());
    }

    @Test
    void parseReadsStatsSectionLowercasingKeys() {
        YamlConfiguration config = configFrom("""
                id: ruby
                type: OFFENSIVE
                stats:
                  DAMAGE: 5
                  Crit_Chance: 2.5
                """);

        Gem gem = parser.parse(config);

        assertEquals("OFFENSIVE", gem.type());
        assertEquals(5.0, gem.statBonus().get("damage"));
        assertEquals(2.5, gem.statBonus().get("crit_chance"));
    }

    @Test
    void parseIgnoresNonNumericStatValues() {
        YamlConfiguration config = configFrom("""
                id: ruby
                stats:
                  damage: not-a-number
                """);

        Gem gem = parser.parse(config);

        assertTrue(gem.statBonus().isEmpty());
    }

    @Test
    void parseReturnsEmptyStatsWhenStatsSectionAbsent() {
        YamlConfiguration config = configFrom("id: ruby");

        assertTrue(parser.parse(config).statBonus().isEmpty());
    }
}

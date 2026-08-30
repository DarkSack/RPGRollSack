package com.sack.rpgroll.items.rarity;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RarityParserTest {

    private final RarityParser parser = new RarityParser();

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
        YamlConfiguration config = configFrom("display-name: Legendary");

        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void parseFallsBackToIdWhenDisplayNameMissing() {
        YamlConfiguration config = configFrom("id: mythic");

        Rarity rarity = parser.parse(config);

        assertEquals("mythic", rarity.id());
        assertEquals("mythic", rarity.displayName());
    }

    @Test
    void parseResolvesNamedColorCaseInsensitively() {
        YamlConfiguration config = configFrom("id: common\ncolor: GoLd");

        Rarity rarity = parser.parse(config);

        assertEquals(NamedTextColor.GOLD, rarity.color());
    }

    @Test
    void parseResolvesHexColor() {
        YamlConfiguration config = configFrom("id: custom\ncolor: '#123456'");

        Rarity rarity = parser.parse(config);

        assertEquals(TextColor.fromHexString("#123456"), rarity.color());
    }

    @Test
    void parseFallsBackToWhiteForUnrecognizedColor() {
        YamlConfiguration config = configFrom("id: broken\ncolor: not-a-color");

        Rarity rarity = parser.parse(config);

        assertEquals(NamedTextColor.WHITE, rarity.color());
    }

    @Test
    void parseDefaultsGlowToFalseWhenAbsent() {
        YamlConfiguration config = configFrom("id: common");

        assertFalse(parser.parse(config).glow());
    }

    @Test
    void parseReadsGlowWhenPresent() {
        YamlConfiguration config = configFrom("id: legendary\nglow: true");

        assertTrue(parser.parse(config).glow());
    }

    @Test
    void parseReturnsNullSoundAndParticleWhenFieldsAbsent() {
        // Sound/Particle are registry-backed pseudo-enums in Paper 26 that
        // require a live server to resolve valueOf() against a real name —
        // exercising that branch needs an integration test, not a unit
        // test. Here we only verify the null-shortcut path taken when the
        // raw config value is absent, which never touches the registry.
        YamlConfiguration config = configFrom("id: common");

        Rarity rarity = parser.parse(config);

        assertNull(rarity.sound());
        assertNull(rarity.particle());
    }
}

package com.sack.rpgroll.mobs.size;

import com.sack.rpgroll.mobs.size.RandomSizeSettings.Category;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomSizeSettingsTest {

    private RandomSizeSettings fromYaml(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return RandomSizeSettings.from(config.getConfigurationSection("random-size"));
    }

    private RandomSizeSettings bundled() throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/config.yml"), StandardCharsets.UTF_8)) {
            return RandomSizeSettings.from(YamlConfiguration.loadConfiguration(reader).getConfigurationSection("random-size"));
        }
    }

    @Test
    void bundledConfigIsOffAndReadsEverySection() throws Exception {

        RandomSizeSettings settings = bundled();

        assertFalse(settings.enabled(), "en el jar viene apagado: se enciende en cada servidor");
        assertTrue(settings.rpgrollMobs());
        assertFalse(settings.rpgrollModeled());
        assertTrue(settings.disabledWorlds().contains("lobby"));
        assertTrue(settings.spawnReasons().contains("NATURAL"));
        assertFalse(settings.spawnReasons().contains("CUSTOM"), "los de plugins van por su propio camino");
        assertTrue(settings.excludedTypes().contains("ender_dragon"));
        assertEquals(new RandomSizeSettings.Range(0.85, 1.3), settings.ranges().get(Category.HOSTILE));
        assertEquals(new RandomSizeSettings.Range(0.92, 1.08), settings.types().get("villager"));
        assertEquals(-0.25, settings.speedWeight());
    }

    @Test
    void appliesOnlyWhenEnabledInAllowedWorldReasonAndType() throws Exception {

        RandomSizeSettings settings = fromYaml("""
                random-size:
                  enabled: true
                  disabled-worlds: [Lobby]
                  spawn-reasons: [natural]
                  exclude-types: [minecraft:WARDEN]
                """);

        assertTrue(settings.appliesTo("world", "NATURAL", "zombie"));
        assertFalse(settings.appliesTo("lobby", "NATURAL", "zombie"));
        assertFalse(settings.appliesTo("world", "CUSTOM", "zombie"));
        assertFalse(settings.appliesTo("world", "NATURAL", "warden"));
        assertFalse(RandomSizeSettings.disabled().appliesTo("world", "NATURAL", "zombie"));
    }

    @Test
    void sizesStayInsideTheRangeWithoutExtremes() throws Exception {

        RandomSizeSettings settings = fromYaml("""
                random-size:
                  enabled: true
                  ranges:
                    passive: {min: 0.7, max: 1.3}
                """);
        Random random = new Random(7);
        double sum = 0;

        for (int i = 0; i < 2000; i++) {
            double scale = settings.pick(Category.PASSIVE, "cow", 1.4, random);
            assertTrue(scale >= 0.7 && scale <= 1.3, "fuera del rango: " + scale);
            sum += scale;
        }

        assertEquals(1.0, sum / 2000, 0.02, "la distribución se centra en el medio del rango");
    }

    @Test
    void ownTypeRangeWinsAndHasNoExtremes() throws Exception {

        RandomSizeSettings settings = fromYaml("""
                random-size:
                  enabled: true
                  extremes: {chance: 100, small: 0.5, large: 2}
                  types:
                    villager: {min: 1, max: 1}
                    "rpgroll:goblin": {min: 1.5, max: 1.5}
                """);
        Random random = new Random(1);

        assertEquals(1.0, settings.pick(Category.PASSIVE, "VILLAGER", 1.95, random), 1e-9);
        assertEquals(1.5, settings.pick(Category.HOSTILE, "rpgroll:goblin", 1.95, random), 1e-9);

        double other = settings.pick(Category.PASSIVE, "cow", 1.4, random);
        assertTrue(other == 0.5 || other == 2.0, "con chance 100 todo lo demás es extremo: " + other);
    }

    @Test
    void tallHostilesNeverShrinkBelowTheMinimumHeight() throws Exception {

        RandomSizeSettings settings = fromYaml("""
                random-size:
                  enabled: true
                  extremes: {chance: 100, small: 0.3, large: 0.3}
                  hostile-min-height: 1.05
                """);
        Random random = new Random(3);

        double zombie = settings.pick(Category.HOSTILE, "zombie", 1.95, random);
        assertEquals(1.05, 1.95 * zombie, 1e-9, "un zombi no pasa por un hueco de un bloque");

        double spider = settings.pick(Category.HOSTILE, "spider", 0.9, random);
        assertEquals(0.3, spider, 1e-9, "la araña ya pasaba: puede encoger");

        double cow = settings.pick(Category.PASSIVE, "cow", 1.4, random);
        assertEquals(0.3, cow, 1e-9, "los pasivos no tienen mínimo");
    }

    @Test
    void factorFollowsSizeByWeightAndNeverGoesNegative() {
        assertEquals(1.8, RandomSizeSettings.factor(1.8, 1), 1e-9);
        assertEquals(1.4, RandomSizeSettings.factor(1.8, 0.5), 1e-9);
        assertEquals(0.8, RandomSizeSettings.factor(1.8, -0.25), 1e-9);
        assertEquals(1.0, RandomSizeSettings.factor(1.8, 0), 1e-9);
        assertEquals(0.05, RandomSizeSettings.factor(0.1, 5), 1e-9);
    }

}

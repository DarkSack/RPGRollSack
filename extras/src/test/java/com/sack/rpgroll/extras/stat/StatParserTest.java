package com.sack.rpgroll.extras.stat;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatParserTest {

    private final StatParser parser = new StatParser();

    private StatDefinition parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return parser.parse(config);
    }

    @Test
    void parsesRestoreAndResetOnDeath() throws InvalidConfigurationException {

        StatDefinition stat = parse("""
                id: thirst
                reset-on-death: false
                restore:
                  water_bottle: 40
                  Melon_Slice: 6
                """);

        assertEquals(Map.of("water_bottle", 40.0, "melon_slice", 6.0), stat.restore());
        assertFalse(stat.resetOnDeath());
    }

    @Test
    void withoutRestoreNothingRestoresAndResetsOnDeath() throws InvalidConfigurationException {

        StatDefinition stat = parse("id: stamina\n");

        assertTrue(stat.restore().isEmpty());
        assertTrue(stat.resetOnDeath());
    }

    /** La sed que se distribuye tiene que poder recuperarse y no vaciarse en minutos. */
    @Test
    void shippedThirstCanBeRestoredAndDecaysSlowly() throws Exception {

        YamlConfiguration config;
        try (var in = getClass().getResourceAsStream("/stats/thirst.yml")) {
            assertNotNull(in);
            config = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        StatDefinition thirst = parser.parse(config);

        assertTrue(thirst.restore().getOrDefault("water_bottle", 0.0) > 0);
        assertNotNull(thirst.decay());
        // De lleno a vacío en al menos una hora de juego.
        double ticksToEmpty = thirst.max() / thirst.decay().amount() * thirst.decay().intervalTicks();
        assertTrue(ticksToEmpty >= 20 * 60 * 60, "la sed se vacía en " + ticksToEmpty / 20 / 60 + " minutos");
    }

}

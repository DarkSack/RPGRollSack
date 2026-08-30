package com.sack.rpgroll.chat.emote;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmoteParserTest {

    private final EmoteParser parser = new EmoteParser();

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
        YamlConfiguration config = load("template: '{player} waves.'");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalEmoteFallsBackToDefaultTemplate() throws Exception {
        YamlConfiguration config = load("id: wave");
        EmoteDefinition def = parser.parse(config);

        assertEquals("{player} hace una acción.", def.template());
        assertNull(def.targetTemplate());
        assertEquals(0.0, def.radius());
    }

    @Test
    void parsesTemplateTargetAndRadius() throws Exception {
        YamlConfiguration config = load("""
                id: wave
                template: "{player} waves."
                target-template: "{player} waves at {target}."
                radius: 20
                """);

        EmoteDefinition def = parser.parse(config);

        assertEquals("{player} waves.", def.template());
        assertEquals("{player} waves at {target}.", def.targetTemplate());
        assertEquals(20.0, def.radius());
    }
}

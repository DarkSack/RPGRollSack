package com.sack.rpgroll.npcs.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcParserTest {

    private final NpcParser parser = new NpcParser();

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
        YamlConfiguration config = load("""
                location:
                  world: world
                """);
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void missingLocationWorldThrows() {
        YamlConfiguration config = load("id: merchant");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void parsesMinimalNpcWithDefaults() {
        YamlConfiguration config = load("""
                id: merchant
                location:
                  world: world
                  x: 10
                  y: 64
                  z: 5
                """);

        NpcDefinition def = parser.parse(config);

        assertEquals("merchant", def.id());
        assertEquals("merchant", def.displayName());
        assertEquals("STANDING", def.pose());
        assertEquals("world", def.world());
        assertEquals(10.0, def.x());
        assertEquals(64.0, def.y());
        assertEquals(5.0, def.z());
        assertTrue(def.actions().isEmpty());
    }

    @Test
    void parsesActionsSkippingInvalidTypeAndIncompleteEntries() {
        YamlConfiguration config = load("""
                id: merchant
                location:
                  world: world
                actions:
                  - type: MESSAGE
                    value: hello
                  - type: NOT_A_TYPE
                    value: bad
                  - type: COMMAND
                """);

        NpcDefinition def = parser.parse(config);

        assertEquals(1, def.actions().size());
        assertEquals(NpcAction.NpcActionType.MESSAGE, def.actions().get(0).type());
        assertEquals("hello", def.actions().get(0).value());
    }

    @Test
    void parsesSkinFieldsAndCustomPose() {
        YamlConfiguration config = load("""
                id: merchant
                pose: sitting
                skin:
                  value: base64value
                  signature: sig
                location:
                  world: world
                """);

        NpcDefinition def = parser.parse(config);

        assertEquals("base64value", def.skinValue());
        assertEquals("sig", def.skinSignature());
        assertEquals("SITTING", def.pose());
        assertTrue(def.hasCustomSkin());
    }
}

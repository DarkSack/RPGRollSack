package com.sack.rpgroll.traps.core;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrapParserTest {

    private final TrapParser parser = new TrapParser();

    @Test
    void parsesFullDefinitionWithBlockAndDisguise() throws Exception {
        YamlConfiguration config = load("""
                id: landmine
                display-name: "Landmine"
                description: "Explota al pisarla"
                icon: TNT
                trigger:
                  type: pressure
                  params:
                    delay-ticks: "5"
                radius: 2.0
                conditions:
                  - "player.sneaking == false"
                actions:
                  - type: explosion
                    params:
                      power: "3.0"
                cooldown-millis: 5000
                charges: 3
                chain:
                  - "other-trap"
                block:
                  material: TNT
                  breakable: false
                  required-item-id: "defuse-kit"
                  break-time-seconds: 10
                  explosion-immune: true
                  piston-immune: true
                disguise:
                  mode: BLOCK_SWAP
                  visible-material: DIRT
                  revealed-material: TNT
                """);

        TrapDefinition def = parser.parse(config);

        assertEquals("landmine", def.id());
        assertEquals("Landmine", def.displayName());
        assertEquals(Material.TNT, def.icon());
        assertEquals(TrapTrigger.PRESSURE, def.trigger());
        assertEquals("5", def.triggerParam("delay-ticks", null));
        assertEquals(2.0, def.radius());
        assertEquals(1, def.conditions().size());
        assertEquals(1, def.actions().size());
        assertEquals("EXPLOSION", def.actions().get(0).type());
        assertEquals(5000, def.cooldownMillis());
        assertEquals(3, def.charges());
        assertEquals(1, def.chain().size());

        assertTrue(def.block().isConfigured());
        assertFalse(def.block().breakable());
        assertTrue(def.block().requiresKey());
        assertEquals(10, def.block().breakTimeSeconds());
        assertTrue(def.block().explosionImmune());
        assertTrue(def.block().pistonImmune());

        assertEquals(TrapDisguiseConfig.DisguiseMode.BLOCK_SWAP, def.disguise().mode());
        assertTrue(def.disguise().isActive());
        assertEquals(Material.DIRT, def.disguise().visibleMaterial());
        assertEquals(Material.TNT, def.disguise().revealedMaterial());
    }

    @Test
    void missingIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("trigger:\n  type: pressure")));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("id: '   '")));
    }

    @Test
    void invalidTriggerTypeThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
                id: bad-trigger
                trigger:
                  type: not-a-real-trigger
                """)));
    }

    @Test
    void missingTriggerTypeDefaultsToPressure() throws Exception {
        TrapDefinition def = parser.parse(load("id: no-trigger"));

        assertEquals(TrapTrigger.PRESSURE, def.trigger());
    }

    @Test
    void invalidIconMaterialFallsBackToTripwireHook() throws Exception {
        TrapDefinition def = parser.parse(load("""
                id: bad-icon
                icon: NOT_A_REAL_MATERIAL
                """));

        assertEquals(Material.TRIPWIRE_HOOK, def.icon());
    }

    @Test
    void missingBlockSectionYieldsNoneConfig() throws Exception {
        TrapDefinition def = parser.parse(load("id: no-block"));

        assertFalse(def.block().isConfigured());
    }

    @Test
    void missingDisguiseSectionYieldsNoneConfig() throws Exception {
        TrapDefinition def = parser.parse(load("id: no-disguise"));

        assertEquals(TrapDisguiseConfig.DisguiseMode.NONE, def.disguise().mode());
        assertFalse(def.disguise().isActive());
    }

    @Test
    void invalidDisguiseModeFallsBackToNone() throws Exception {
        TrapDefinition def = parser.parse(load("""
                id: bad-disguise
                disguise:
                  mode: NOT_A_MODE
                """));

        assertEquals(TrapDisguiseConfig.DisguiseMode.NONE, def.disguise().mode());
    }

    @Test
    void actionEntryWithoutTypeIsSkipped() throws Exception {
        TrapDefinition def = parser.parse(load("""
                id: skip-action
                actions:
                  - params:
                      value: "x"
                """));

        assertTrue(def.actions().isEmpty());
    }

    @Test
    void actionTypeIsUppercasedRegardlessOfYamlCasing() throws Exception {
        TrapDefinition def = parser.parse(load("""
                id: lower-action
                actions:
                  - type: message
                    params:
                      value: "hi"
                """));

        assertEquals("MESSAGE", def.actions().get(0).type());
        assertEquals("hi", def.actions().get(0).param("value", null));
    }

    private YamlConfiguration load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}

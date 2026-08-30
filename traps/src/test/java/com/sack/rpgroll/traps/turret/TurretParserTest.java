package com.sack.rpgroll.traps.turret;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurretParserTest {

    private final TurretParser parser = new TurretParser();

    @Test
    void parsesFullDefinitionWithCustomImpact() throws Exception {
        YamlConfiguration config = load("""
                id: crossbow-turret
                display-name: "Crossbow Turret"
                description: "Fires bolts at intruders"
                radius: 20.0
                target-players: false
                target-hostile-mobs: true
                fire-interval-ticks: 40
                conditions:
                  - "night == true"
                impact:
                  type: custom_projectile
                  params:
                    projectile-type: "TRIDENT"
                    speed: "3.0"
                model:
                  material: DISPENSER
                  custom-model-data: 7
                """);

        TurretDefinition def = parser.parse(config);

        assertEquals("crossbow-turret", def.id());
        assertEquals(20.0, def.radius());
        assertFalse(def.targetPlayers());
        assertTrue(def.targetHostileMobs());
        assertEquals(40, def.fireIntervalTicks());
        assertEquals(1, def.conditions().size());
        assertEquals("CUSTOM_PROJECTILE", def.impact().type());
        assertEquals("TRIDENT", def.impact().param("projectile-type", null));
        assertEquals(Material.DISPENSER, def.model());
        assertEquals(7, def.customModelData());
    }

    @Test
    void missingIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("radius: 10")));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(load("id: '  '")));
    }

    @Test
    void missingImpactSectionDefaultsToArrowProjectile() throws Exception {
        TurretDefinition def = parser.parse(load("id: default-turret"));

        assertEquals("CUSTOM_PROJECTILE", def.impact().type());
        assertEquals("ARROW", def.impact().param("projectile-type", null));
    }

    @Test
    void missingCustomModelDataIsNull() throws Exception {
        TurretDefinition def = parser.parse(load("id: no-cmd"));

        assertNull(def.customModelData());
    }

    @Test
    void invalidModelMaterialFallsBackToCrossbow() throws Exception {
        TurretDefinition def = parser.parse(load("""
                id: bad-model
                model:
                  material: NOT_A_MATERIAL
                """));

        assertEquals(Material.CROSSBOW, def.model());
    }

    @Test
    void defaultsApplyWhenOptionalFieldsOmitted() throws Exception {
        TurretDefinition def = parser.parse(load("id: minimal"));

        assertEquals(12.0, def.radius());
        assertTrue(def.targetPlayers());
        assertTrue(def.targetHostileMobs());
        assertEquals(20, def.fireIntervalTicks());
        assertTrue(def.conditions().isEmpty());
    }

    private YamlConfiguration load(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config;
    }
}

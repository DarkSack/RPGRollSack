package com.sack.rpgroll.mobs.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobParserTest {

    private final MobParser parser = new MobParser();

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
        YamlConfiguration config = load("base-entity-type: ZOMBIE");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void missingBaseEntityTypeThrows() {
        YamlConfiguration config = load("id: goblin");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalDefinitionFallsBackToDefaults() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                """);

        MobDefinition def = parser.parse(config);

        assertEquals("goblin", def.id());
        assertEquals(MobCategory.NORMAL, def.category());
        assertEquals("goblin", def.displayName());
        assertEquals(1, def.level());
        assertEquals("common", def.rarityId());
        assertEquals("ZOMBIE", def.model().baseEntityType());
        assertTrue(def.stats().isEmpty());
        assertTrue(def.skills().isEmpty());
        assertTrue(def.phases().isEmpty());
    }

    @Test
    void unknownCategoryFallsBackToNormal() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                category: NOT_A_CATEGORY
                """);

        assertEquals(MobCategory.NORMAL, parser.parse(config).category());
    }

    @Test
    void parsesStatsAndResistancesAsLowercaseNumberMaps() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                stats:
                  HEALTH: 50
                  Damage: 5
                resistances:
                  FIRE: 20
                """);

        MobDefinition def = parser.parse(config);

        assertEquals(50.0, def.stat("health"));
        assertEquals(5.0, def.stat("damage"));
        assertEquals(20.0, def.resistances().get("fire"));
        assertEquals(0.0, def.stat("unknown"));
    }

    @Test
    void parsesWeaknessesSkippingInvalidEntries() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                weaknesses:
                  - type: ELEMENT
                    key: fire
                    extra-damage-percent: 50
                  - type: NOT_A_TYPE
                    key: bad
                  - key: missing-type
                """);

        MobDefinition def = parser.parse(config);

        assertEquals(1, def.weaknesses().size());
        assertEquals(50.0, def.weaknesses().get(0).extraDamagePercent());
    }

    @Test
    void parsesSkillsWithCooldownAndDefaultChance() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                skills:
                  - id: fireball
                    trigger: ATTACK
                    cooldown: 5s
                    actions:
                      - DAMAGE
                """);

        MobDefinition def = parser.parse(config);

        assertEquals(1, def.skills().size());
        MobSkill skill = def.skills().get(0);
        assertEquals("fireball", skill.id());
        assertEquals(MobTrigger.ATTACK, skill.trigger());
        assertEquals(5000L, skill.cooldownMillis());
        assertEquals(100.0, skill.chance());
        assertEquals(1, skill.actions().size());
        assertEquals("DAMAGE", skill.actions().get(0).type());
    }

    @Test
    void skillWithInvalidTriggerFallsBackToNull() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                skills:
                  - id: fireball
                    trigger: NOT_REAL
                """);

        assertEquals(null, parser.parse(config).skills().get(0).trigger());
    }

    @Test
    void parsesPhasesWithStatMultipliersAndThreshold() {
        YamlConfiguration config = load("""
                id: boss
                base-entity-type: wither_skeleton
                phases:
                  - id: enraged
                    health-threshold-percent: 50
                    stat-multipliers:
                      DAMAGE: 1.5
                """);

        MobDefinition def = parser.parse(config);

        assertEquals(1, def.phases().size());
        MobPhase phase = def.phases().get(0);
        assertEquals(50.0, phase.healthThresholdPercent());
        assertEquals(1.5, phase.statMultipliers().get("damage"));
    }

    @Test
    void parsesLootDefaultingAmountMaxToAmountMinAndSkipsInvalidType() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                loot:
                  - type: ITEM
                    reference: gold-coin
                    amount-min: 2
                    chance: 30
                  - type: NOT_A_TYPE
                    reference: bad
                """);

        MobDefinition def = parser.parse(config);

        assertEquals(1, def.loot().size());
        MobLootEntry entry = def.loot().get(0);
        assertEquals(2, entry.amountMin());
        assertEquals(2, entry.amountMax());
        assertEquals(30.0, entry.chance());
    }

    @Test
    void parsesSpawnRulesHourRange() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                spawn-rules:
                  hour: 13000-23000
                  natural-spawn: true
                """);

        SpawnRules rules = parser.parse(config).spawnRules();

        assertEquals(13000, rules.hourMin());
        assertEquals(23000, rules.hourMax());
        assertTrue(rules.hasTimeRange());
        assertTrue(rules.naturalSpawn());
    }

    @Test
    void spawnRulesWithoutHourRangeHasNoTimeRange() {
        YamlConfiguration config = load("""
                id: goblin
                base-entity-type: zombie
                """);

        assertFalse(parser.parse(config).spawnRules().hasTimeRange());
    }
}

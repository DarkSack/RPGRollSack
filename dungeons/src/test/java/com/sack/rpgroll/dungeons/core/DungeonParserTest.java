package com.sack.rpgroll.dungeons.core;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonParserTest {

    private final DungeonParser parser = new DungeonParser();

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
        YamlConfiguration config = load("category: misc");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(config));
    }

    @Test
    void minimalDefinitionFallsBackToDefaults() {
        YamlConfiguration config = load("id: crypt");
        DungeonDefinition def = parser.parse(config);

        assertEquals("crypt", def.id());
        assertEquals("misc", def.category());
        assertEquals("crypt", def.displayName());
        assertEquals(1, def.recommendedLevel());
        assertEquals(1, def.difficulties().size());
        assertEquals("normal", def.difficulties().get(0).id());
        assertTrue(def.rooms().isEmpty());
    }

    @Test
    void parsesRoomsWithWavesAndObjectives() {
        YamlConfiguration config = load("""
                id: crypt
                rooms:
                  - id: room1
                    type: COMBAT
                    boss: skeleton-king
                    objectives:
                      - type: KILL_ENTITY
                        amount: 5
                    waves:
                      - id: wave1
                        time-limit: 30s
                        mobs:
                          - id: zombie
                            amount: 3
                """);

        DungeonDefinition def = parser.parse(config);

        assertEquals(1, def.rooms().size());
        DungeonRoom room = def.rooms().get(0);
        assertEquals(DungeonRoomType.COMBAT, room.type());
        assertTrue(room.hasBoss());
        assertEquals(1, room.objectives().size());
        assertEquals(5, room.objectives().get(0).amount());
        assertEquals(1, room.waves().size());
        DungeonWave wave = room.waves().get(0);
        assertEquals(30_000L, wave.timeLimitMillis());
        assertEquals(3, wave.totalMobCount());
    }

    @Test
    void unknownRoomTypeFallsBackToCombat() {
        YamlConfiguration config = load("""
                id: crypt
                rooms:
                  - id: room1
                    type: NOT_A_TYPE
                """);

        assertEquals(DungeonRoomType.COMBAT, parser.parse(config).rooms().get(0).type());
    }

    @Test
    void roomWithoutBossHasNoBoss() {
        YamlConfiguration config = load("""
                id: crypt
                rooms:
                  - id: room1
                """);

        assertFalseHasBoss(parser.parse(config).rooms().get(0));
    }

    private void assertFalseHasBoss(DungeonRoom room) {
        assertEquals(false, room.hasBoss());
    }

    @Test
    void parsesDifficultiesWithModifiers() {
        YamlConfiguration config = load("""
                id: crypt
                difficulties:
                  - id: hard
                    health-multiplier: 2.0
                    modifiers:
                      - NO_HEALING
                      - NOT_A_MODIFIER
                """);

        DungeonDifficulty difficulty = parser.parse(config).difficulties().get(0);

        assertEquals("hard", difficulty.id());
        assertEquals(2.0, difficulty.healthMultiplier());
        assertEquals(1, difficulty.modifiers().size());
        assertTrue(difficulty.hasModifier(DungeonModifierType.NO_HEALING));
    }

    @Test
    void parsesCheckpointPolicyAndRevive() {
        YamlConfiguration config = load("""
                id: crypt
                checkpoints:
                  mode: RESTART_ROOM
                  shared-lives: true
                  max-retries: 3
                revive:
                  mode: WITH_ITEM
                  timer: 10s
                  item: TOTEM_OF_UNDYING
                """);

        DungeonDefinition def = parser.parse(config);

        assertEquals(CheckpointMode.RESTART_ROOM, def.checkpointPolicy().mode());
        assertTrue(def.checkpointPolicy().sharedLives());
        assertEquals(3, def.checkpointPolicy().maxRetries());
        assertEquals(ReviveMode.WITH_ITEM, def.reviveConfig().mode());
        assertEquals(10_000L, def.reviveConfig().reviveTimerMillis());
    }

    @Test
    void parsesLootDefaultingAmountMaxAndSkippingInvalidType() {
        YamlConfiguration config = load("""
                id: crypt
                loot:
                  - type: ITEM
                    reference: gold-coin
                    amount-min: 2
                  - type: NOT_A_TYPE
                    reference: bad
                """);

        assertEquals(1, parser.parse(config).loot().size());
        DungeonLootEntry entry = parser.parse(config).loot().get(0);
        assertEquals(2, entry.amountMin());
        assertEquals(2, entry.amountMax());
    }
}

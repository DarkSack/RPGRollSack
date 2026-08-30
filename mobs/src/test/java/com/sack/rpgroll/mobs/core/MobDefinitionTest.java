package com.sack.rpgroll.mobs.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobDefinitionTest {

    private MobDefinition minimal(List<MobPhase> phases) {
        return new MobDefinition("goblin", MobCategory.NORMAL, null, null, 1, null, null, null,
                MobModel.defaults("ZOMBIE"), null, null, null, null, null, null, phases, null, null, null, null,
                null);
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MobDefinition("", MobCategory.NORMAL, null, null, 1,
                null, null, null, MobModel.defaults("ZOMBIE"), null, null, null, null, null, null, null, null, null,
                null, null, null));
    }

    @Test
    void nullModelIsRejected() {
        assertThrows(NullPointerException.class, () -> new MobDefinition("goblin", MobCategory.NORMAL, null, null, 1,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    void nullDisplayNameFallsBackToId() {
        assertEquals("goblin", minimal(List.of()).displayName());
    }

    @Test
    void levelIsClampedToAtLeastOne() {
        MobDefinition def = new MobDefinition("goblin", MobCategory.NORMAL, null, null, -5, null, null, null,
                MobModel.defaults("ZOMBIE"), null, null, null, null, null, null, null, null, null, null, null, null);

        assertEquals(1, def.level());
    }

    @Test
    void statReturnsZeroForUnknownKey() {
        MobDefinition def = new MobDefinition("goblin", MobCategory.NORMAL, null, null, 1, null, null, null,
                MobModel.defaults("ZOMBIE"), Map.of("health", 50.0), null, null, null, null, null, null, null, null,
                null, null, null);

        assertEquals(50.0, def.stat("health"));
        assertEquals(0.0, def.stat("missing"));
    }

    @Test
    void isBossTrueOnlyForBossCategories() {
        for (MobCategory category : MobCategory.values()) {
            MobDefinition def = new MobDefinition("id", category, null, null, 1, null, null, null,
                    MobModel.defaults("ZOMBIE"), null, null, null, null, null, null, null, null, null, null, null,
                    null);

            boolean expected = category == MobCategory.MINI_BOSS || category == MobCategory.WORLD_BOSS
                    || category == MobCategory.RAID_BOSS || category == MobCategory.DUNGEON_BOSS
                    || category == MobCategory.CINEMATIC_BOSS;

            assertEquals(expected, def.isBoss(), "category " + category);
        }
    }

    @Test
    void phaseForReturnsEmptyWhenNoPhaseThresholdReached() {
        MobPhase phase = new MobPhase("enraged", 30, null, null, null, null, null, null);
        MobDefinition def = minimal(List.of(phase));

        assertTrue(def.phaseFor(50).isEmpty());
    }

    @Test
    void phaseForPicksDeepestCrossedThreshold() {
        MobPhase enraged = new MobPhase("enraged", 50, null, null, null, null, null, null);
        MobPhase desperate = new MobPhase("desperate", 20, null, null, null, null, null, null);
        MobDefinition def = minimal(List.of(enraged, desperate));

        Optional<MobPhase> at40 = def.phaseFor(40);
        assertTrue(at40.isPresent());
        assertEquals("enraged", at40.get().id());

        Optional<MobPhase> at10 = def.phaseFor(10);
        assertTrue(at10.isPresent());
        assertEquals("desperate", at10.get().id());
    }

    @Test
    void phaseForAtExactThresholdIsInclusive() {
        MobPhase phase = new MobPhase("enraged", 50, null, null, null, null, null, null);
        MobDefinition def = minimal(List.of(phase));

        assertFalse(def.phaseFor(50.0001).isPresent());
        assertTrue(def.phaseFor(50.0).isPresent());
    }
}

package com.sack.rpgroll.fishing.minigame;

import com.sack.rpgroll.fishing.core.FishBehaviorType;
import com.sack.rpgroll.fishing.core.FishCategory;
import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.FishSpecies;
import com.sack.rpgroll.fishing.core.CatchQuality;
import com.sack.rpgroll.fishing.engine.CatchResult;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishBattleSessionTest {

    private static FishBattleSession session(FishBehaviorType behavior, double resistance, double reelSpeed) {
        FishSpecies species = new FishSpecies("cod", null, null, 0, null, FishCategory.FRESHWATER, FishRarity.COMMON,
                Set.of(), Set.of(), Set.of(), 1, 2, 1, 2, 10, 5, behavior, Set.of(), Set.of(), Set.of(), Set.of(),
                false, 0, false, null, null, null);
        CatchResult result = CatchResult.fish(species, 1.5, 1.5, CatchQuality.COMMON, 10, 5);
        return new FishBattleSession(result, resistance, reelSpeed, new Random(1));
    }

    /** Avanza hasta que el indicador esté (o no) dentro de la zona. */
    private static void stepUntil(FishBattleSession s, boolean inZone) {
        for (int i = 0; i < 200 && s.isInZone(s.meterPosition()) != inZone; i++) {
            s.tick();
        }
        assertEquals(inZone, s.isInZone(s.meterPosition()));
    }

    @Test
    void onlyOneHitCountsPerPassThroughTheZone() {
        FishBattleSession s = session(FishBehaviorType.SLOW, 1, 1);
        int before = s.requiredHits();

        assertTrue(s.isInZone(s.meterPosition()));
        assertEquals(FishBattleSession.Swing.HIT, s.swing());
        assertEquals(FishBattleSession.Swing.IGNORED, s.swing());
        assertEquals(FishBattleSession.Swing.IGNORED, s.swing());
        assertEquals(before - 1, s.requiredHits());

        stepUntil(s, false);
        stepUntil(s, true);
        assertEquals(FishBattleSession.Swing.HIT, s.swing());
        assertEquals(before - 2, s.requiredHits());
    }

    @Test
    void swingOutsideTheZoneCostsAMissAndEnoughMissesLose() {
        FishBattleSession s = session(FishBehaviorType.SLOW, 1, 1);
        stepUntil(s, false);

        int misses = s.allowedMisses();
        for (int i = 0; i <= misses; i++) {
            assertFalse(s.isLost());
            assertEquals(FishBattleSession.Swing.MISS, s.swing());
        }
        assertTrue(s.isLost());
    }

    @Test
    void reelSpeedWidensOrNarrowsTheZoneWithinLimits() {
        double normal = session(FishBehaviorType.AGGRESSIVE, 1, 1).zoneHalfWidth();

        assertEquals(normal * 1.5, session(FishBehaviorType.AGGRESSIVE, 1, 1.5).zoneHalfWidth(), 1e-9);
        assertEquals(normal * 0.5, session(FishBehaviorType.AGGRESSIVE, 1, 0.1).zoneHalfWidth(), 1e-9);
        assertTrue(session(FishBehaviorType.SLOW, 1, 50).zoneHalfWidth() <= 0.4);
    }

    @Test
    void resistanceGivesMoreSpareMisses() {
        assertTrue(session(FishBehaviorType.SLOW, 2.2, 1).allowedMisses()
                > session(FishBehaviorType.SLOW, 1, 1).allowedMisses());
    }

    @Test
    void theFishEscapesOnItsOwnAfterTheTimeLimit() {
        FishBattleSession s = session(FishBehaviorType.SLOW, 1, 1);
        for (int i = 0; i < FishBattleSession.MAX_DURATION_STEPS; i++) {
            assertFalse(s.isLost());
            s.tick();
        }
        assertTrue(s.isLost());
    }

}

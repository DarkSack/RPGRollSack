package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.ascension.deferred.Faction;
import com.sack.rpgroll.ascension.deferred.FactionRank;
import com.sack.rpgroll.ascension.reward.Rewards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReputationMathTest {

    private static FactionRank rank(String id, int threshold) {
        return new FactionRank(id, id, threshold, Rewards.none());
    }

    // Desordenados a propósito: la facción los ordena por umbral.
    private static final Faction KINGDOM = new Faction("kingdom", "Reino",
            List.of(rank("aliado", 500), rank("hostil", -5000), rank("heroe", 5000), rank("neutral", 0),
                    rank("sospechoso", -500)),
            List.of(), List.of("orden"), 0.25, -5000, 10000);

    @Test
    void rankIsTheHighestThresholdNotAboveReputation() {
        assertEquals("neutral", KINGDOM.rankFor(0).orElseThrow().id());
        assertEquals("neutral", KINGDOM.rankFor(499).orElseThrow().id());
        assertEquals("aliado", KINGDOM.rankFor(500).orElseThrow().id());
        assertEquals("sospechoso", KINGDOM.rankFor(-1).orElseThrow().id());
        assertEquals("hostil", KINGDOM.rankFor(-5000).orElseThrow().id());
        assertTrue(KINGDOM.rankFor(-6000).isEmpty());
    }

    @Test
    void nextRankIsTheFirstAbove() {
        assertEquals("aliado", KINGDOM.nextRank(0).orElseThrow().id());
        assertTrue(KINGDOM.nextRank(5000).isEmpty());
    }

    @Test
    void risingThroughSeveralRanksEntersEachInOrder() {
        List<FactionRank> entered = ReputationMath.ranksEntered(KINGDOM, 0, 6000);
        assertEquals(List.of("aliado", "heroe"), entered.stream().map(FactionRank::id).toList());
    }

    @Test
    void risingWithinARankEntersNothing() {
        assertTrue(ReputationMath.ranksEntered(KINGDOM, 10, 400).isEmpty());
    }

    @Test
    void fallingEntersOnlyTheRankYouLandIn() {
        List<FactionRank> entered = ReputationMath.ranksEntered(KINGDOM, 600, -400);
        assertEquals(List.of("sospechoso"), entered.stream().map(FactionRank::id).toList());
    }

    @Test
    void fallingPastSeveralRanksLandsInTheLowest() {
        List<FactionRank> entered = ReputationMath.ranksEntered(KINGDOM, 600, -600);
        assertEquals(List.of("hostil"), entered.stream().map(FactionRank::id).toList());
    }

    @Test
    void rivalsLoseAFractionOfGainsOnly() {
        assertEquals(25, ReputationMath.rivalLoss(KINGDOM, 100));
        assertEquals(0, ReputationMath.rivalLoss(KINGDOM, -100));
    }

    @Test
    void reputationIsClampedToTheFactionLimits() {
        assertEquals(10000, KINGDOM.clamp(99999));
        assertEquals(-5000, KINGDOM.clamp(-99999));
    }

    @Test
    void minAboveMaxIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Faction("x", "x", List.of(), List.of(), List.of(), 0, 10, -10));
    }

}

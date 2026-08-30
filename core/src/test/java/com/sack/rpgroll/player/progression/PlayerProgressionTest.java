package com.sack.rpgroll.player.progression;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProgressionTest {

    @Test
    void createNewStartsAtLevelOneWithNoExperience() {
        PlayerProgression progression = PlayerProgression.createNew();

        assertEquals(1, progression.level());
        assertEquals(0, progression.experience());
        assertEquals(0, progression.unspentStatPoints());
        assertFalse(progression.isMaxLevel());
    }

    @Test
    void constructorRejectsNegativeUnspentStatPoints() {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerProgression(1, 0, 0L, 0L, -1));
    }

    @Test
    void requiredExpForCurrentLevelIsZeroAtMinLevel() {
        PlayerProgression progression = new PlayerProgression(PlayerProgression.MIN_LEVEL, 0, 0L, 0L, 0);

        assertEquals(0, progression.getRequiredExpForCurrentLevel());
    }

    @Test
    void requiredExpForNextLevelFollowsBaseExpTimesLevelPow15() {
        PlayerProgression progression = new PlayerProgression(1, 0, 0L, 0L, 0);

        int expected = (int) (PlayerProgression.BASE_EXP * Math.pow(2, PlayerProgression.EXP_MULTIPLIER));
        assertEquals(expected, progression.getRequiredExpForNextLevel());
    }

    @Test
    void requiredExpForNextLevelIsMaxIntAtMaxLevel() {
        PlayerProgression progression = new PlayerProgression(PlayerProgression.MAX_LEVEL, 0, 0L, 0L, 0);

        assertEquals(Integer.MAX_VALUE, progression.getRequiredExpForNextLevel());
    }

    @Test
    void expToNextLevelNeverGoesNegativeWhenExperienceExceedsRequirement() {
        PlayerProgression progression = new PlayerProgression(1, Integer.MAX_VALUE, 0L, 0L, 0);

        assertEquals(0, progression.getExpToNextLevel());
    }

    @Test
    void expToNextLevelIsFullRequirementAtZeroExperience() {
        PlayerProgression progression = new PlayerProgression(1, 0, 0L, 0L, 0);

        assertEquals(progression.getRequiredExpForNextLevel(), progression.getExpToNextLevel());
    }

    @Test
    void progressPercentIsZeroRightAfterLevelingUp() {
        int current = new PlayerProgression(2, 0, 0L, 0L, 0).getRequiredExpForCurrentLevel();
        PlayerProgression atCurrentThreshold = new PlayerProgression(2, current, 0L, 0L, 0);

        assertEquals(0, atCurrentThreshold.getProgressPercent());
    }

    @Test
    void progressPercentIsHundredAtMaxLevelRegardlessOfExperience() {
        PlayerProgression progression = new PlayerProgression(PlayerProgression.MAX_LEVEL, 0, 0L, 0L, 0);

        assertEquals(100, progression.getProgressPercent());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 25, 50, 99})
    void progressPercentStaysWithinZeroToHundredAtEveryThresholdBoundary(int level) {
        PlayerProgression progression = new PlayerProgression(level, 0, 0L, 0L, 0);
        int current = progression.getRequiredExpForCurrentLevel();
        int next = progression.getRequiredExpForNextLevel();

        PlayerProgression atStart = new PlayerProgression(level, current, 0L, 0L, 0);
        PlayerProgression atEnd = new PlayerProgression(level, next, 0L, 0L, 0);

        assertEquals(0, atStart.getProgressPercent());
        assertEquals(100, atEnd.getProgressPercent());
    }

    @Test
    void isMaxLevelTrueOnlyAtOrAboveMaxLevel() {
        assertFalse(new PlayerProgression(PlayerProgression.MAX_LEVEL - 1, 0, 0L, 0L, 0).isMaxLevel());
        assertTrue(new PlayerProgression(PlayerProgression.MAX_LEVEL, 0, 0L, 0L, 0).isMaxLevel());
    }

    @Test
    void addStatPointsAccumulatesWithoutMutatingOriginal() {
        PlayerProgression original = PlayerProgression.createNew();
        PlayerProgression updated = original.addStatPoints(3);

        assertEquals(0, original.unspentStatPoints());
        assertEquals(3, updated.unspentStatPoints());
    }

    @Test
    void spendStatPointsDeductsAvailablePoints() {
        PlayerProgression progression = PlayerProgression.createNew().addStatPoints(5);
        PlayerProgression spent = progression.spendStatPoints(2);

        assertEquals(3, spent.unspentStatPoints());
    }

    @Test
    void spendStatPointsRejectsSpendingMoreThanAvailable() {
        PlayerProgression progression = PlayerProgression.createNew().addStatPoints(2);

        assertThrows(IllegalArgumentException.class, () -> progression.spendStatPoints(3));
    }

    @Test
    void withUnspentStatPointsReplacesTotalDirectly() {
        PlayerProgression progression = PlayerProgression.createNew().addStatPoints(10);
        PlayerProgression reset = progression.withUnspentStatPoints(0);

        assertEquals(0, reset.unspentStatPoints());
    }
}

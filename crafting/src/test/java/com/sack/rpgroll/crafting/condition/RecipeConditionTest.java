package com.sack.rpgroll.crafting.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecipeConditionTest {

    @Test
    void levelMinFactorySetsLevelTypeAndMinValue() {
        RecipeCondition condition = RecipeCondition.levelMin(15);

        assertEquals(ConditionType.LEVEL_MIN, condition.type());
        assertNull(condition.value());
        assertEquals(15, condition.minValue());
    }

    @Test
    void ofFactoryLeavesMinValueAtZero() {
        RecipeCondition condition = RecipeCondition.of(ConditionType.WORLD, "world_nether");

        assertEquals(ConditionType.WORLD, condition.type());
        assertEquals("world_nether", condition.value());
        assertEquals(0, condition.minValue());
    }

    @Test
    void jobMinFactorySetsJobIdAndLevel() {
        RecipeCondition condition = RecipeCondition.jobMin("blacksmith", 10);

        assertEquals(ConditionType.JOB_MIN, condition.type());
        assertEquals("blacksmith", condition.value());
        assertEquals(10, condition.minValue());
    }
}

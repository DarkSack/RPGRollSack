package com.sack.rpgroll.quests.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestObjectiveTest {

    @Test
    void constructorClampsAmountToAtLeastOne() {
        assertEquals(1, new QuestObjective("KILL_ENTITY", null, 0, null).amount());
        assertEquals(1, new QuestObjective("KILL_ENTITY", null, -5, null).amount());
        assertEquals(10, new QuestObjective("KILL_ENTITY", null, 10, null).amount());
    }

    @Test
    void constructorDefaultsNullDescriptionAndParams() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1, null);

        assertEquals("", objective.description());
        assertTrue(objective.params().isEmpty());
    }

    @Test
    void matchesReturnsTrueWhenNoParamsDeclared() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1, null);

        assertTrue(objective.matches(Map.of("entity", "ZOMBIE")));
        assertTrue(objective.matches(Map.of()));
    }

    @Test
    void matchesComparesDeclaredParamsCaseInsensitively() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1,
                Map.of("entity", "ZOMBIE"));

        assertTrue(objective.matches(Map.of("entity", "zombie")));
        assertFalse(objective.matches(Map.of("entity", "SKELETON")));
    }

    @Test
    void matchesFailsWhenEventIsMissingADeclaredKey() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1,
                Map.of("entity", "ZOMBIE"));

        assertFalse(objective.matches(Map.of("other-key", "value")));
    }

    @Test
    void matchesIgnoresTheAmountKeyEvenIfDeclared() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1,
                Map.of("amount", "999"));

        assertTrue(objective.matches(Map.of()));
    }

    @Test
    void paramReturnsFallbackWhenKeyMissing() {
        QuestObjective objective = new QuestObjective("KILL_ENTITY", null, 1, Map.of("entity", "ZOMBIE"));

        assertEquals("ZOMBIE", objective.param("entity", "default"));
        assertEquals("default", objective.param("missing", "default"));
    }
}

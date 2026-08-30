package com.sack.rpgroll.ascension.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscensionRequirementsTest {

    @Test
    void noneHasNoRequirementsAtAll() {
        AscensionRequirements requirements = AscensionRequirements.none();

        assertEquals(0, requirements.level());
        assertEquals(0, requirements.prestige());
        assertNull(requirements.trait());
        assertTrue(requirements.completedQuests().isEmpty());
        assertTrue(requirements.reputation().isEmpty());
    }

    @Test
    void nullCollectionsAreNormalizedToEmpty() {
        AscensionRequirements requirements = new AscensionRequirements(5, 1, "brave", null, null);

        assertTrue(requirements.completedQuests().isEmpty());
        assertTrue(requirements.reputation().isEmpty());
    }

    @Test
    void completedQuestsAreDefensivelyCopied() {
        List<String> quests = new ArrayList<>(List.of("intro"));
        AscensionRequirements requirements = new AscensionRequirements(0, 0, null, quests, Map.of());

        quests.add("mutated");

        assertEquals(List.of("intro"), requirements.completedQuests());
    }

    @Test
    void reputationIsDefensivelyCopied() {
        Map<String, Integer> reputation = new HashMap<>(Map.of("guild", 10));
        AscensionRequirements requirements = new AscensionRequirements(0, 0, null, List.of(), reputation);

        reputation.put("mutated", 99);

        assertEquals(Map.of("guild", 10), requirements.reputation());
    }

    @Test
    void completedQuestsListIsImmutable() {
        AscensionRequirements requirements = new AscensionRequirements(0, 0, null, List.of("intro"), Map.of());

        assertThrows(UnsupportedOperationException.class, () -> requirements.completedQuests().add("nope"));
    }

    @Test
    void reputationMapIsImmutable() {
        AscensionRequirements requirements = new AscensionRequirements(0, 0, null, List.of(), Map.of("guild", 1));

        assertThrows(UnsupportedOperationException.class, () -> requirements.reputation().put("nope", 1));
    }
}

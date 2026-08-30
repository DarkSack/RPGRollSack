package com.sack.rpgroll.ascension.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TalentNodeTest {

    private static TalentNode node(List<String> prerequisites, Map<String, Double> statBonus) {
        return new TalentNode("cleave", "Cleave", 2, prerequisites, statBonus, null, null, null);
    }

    @Test
    void nullPrerequisitesAndStatBonusAreNormalizedToEmpty() {
        TalentNode talent = node(null, null);

        assertTrue(talent.prerequisites().isEmpty());
        assertTrue(talent.statBonus().isEmpty());
    }

    @Test
    void prerequisitesAreDefensivelyCopied() {
        List<String> prerequisites = new ArrayList<>(List.of("root"));
        TalentNode talent = node(prerequisites, Map.of());

        prerequisites.add("mutated");

        assertEquals(List.of("root"), talent.prerequisites());
    }

    @Test
    void statBonusIsDefensivelyCopied() {
        Map<String, Double> statBonus = new HashMap<>(Map.of("strength", 2.0));
        TalentNode talent = node(List.of(), statBonus);

        statBonus.put("mutated", 99.0);

        assertEquals(Map.of("strength", 2.0), talent.statBonus());
    }

    @Test
    void prerequisitesListIsImmutable() {
        TalentNode talent = node(List.of("root"), Map.of());

        assertThrows(UnsupportedOperationException.class, () -> talent.prerequisites().add("nope"));
    }

    @Test
    void statBonusMapIsImmutable() {
        TalentNode talent = node(List.of(), Map.of("strength", 1.0));

        assertThrows(UnsupportedOperationException.class, () -> talent.statBonus().put("nope", 1.0));
    }

    @Test
    void grantedRewardsArePreservedAsGiven() {
        TalentNode talent = new TalentNode("cleave", "Cleave", 3, List.of(), Map.of(),
                "whirlwind", "brutal", "sharpness");

        assertEquals("whirlwind", talent.grantedSkill());
        assertEquals("brutal", talent.grantedTrait());
        assertEquals("sharpness", talent.grantedEnchantment());
        assertEquals(3, talent.cost());
    }
}

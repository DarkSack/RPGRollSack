package com.sack.rpgroll.ascension.progress;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriterionTest {

    @Test
    void globMatchesWildcardsCaseInsensitively() {
        assertTrue(Glob.matches("*_ORE", "DIAMOND_ORE"));
        assertTrue(Glob.matches("*_ore", "DEEPSLATE_IRON_ORE"));
        assertTrue(Glob.matches("zombie", "ZOMBIE"));
        assertFalse(Glob.matches("ZOMBIE", "ZOMBIE_VILLAGER"));
        assertFalse(Glob.matches("*_ORE", "ORE_BLOCK"));
    }

    @Test
    void emptyStarAndAnyMatchEverything() {
        assertTrue(Glob.matches(null, "X"));
        assertTrue(Glob.matches("", "X"));
        assertTrue(Glob.matches("*", "X"));
        assertTrue(Glob.matches("any", null));
    }

    @Test
    void globPartsAreLiteralNotRegex() {
        // Un punto en el patrón es un punto, no "cualquier carácter".
        assertTrue(Glob.matches("minecraft:*", "minecraft:plains"));
        assertFalse(Glob.matches("a.c", "abc"));
    }

    @Test
    void criterionMatchesTypeTargetAndHeldItem() {
        Criterion swordKills = new Criterion(TriggerType.KILL_ENTITY, "*", "*_SWORD", null, 10);

        assertTrue(swordKills.matches(new ProgressEvent(TriggerType.KILL_ENTITY, "ZOMBIE", "DIAMOND_SWORD")));
        assertFalse(swordKills.matches(new ProgressEvent(TriggerType.KILL_ENTITY, "ZOMBIE", "BOW")));
        assertFalse(swordKills.matches(new ProgressEvent(TriggerType.KILL_ENTITY, "ZOMBIE", null)));
        assertFalse(swordKills.matches(new ProgressEvent(TriggerType.BREAK_BLOCK, "STONE", "DIAMOND_SWORD")));
    }

    @Test
    void stateTriggersCannotBeEvents() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProgressEvent(TriggerType.REACH_LEVEL, "30", null));
    }

    @Test
    void parserReadsEntriesAndDefaultsAmountToOne() {
        List<Criterion> criteria = CriterionParser.parseList("test", List.of(
                Map.of("type", "kill_entity", "target", "ZOMBIE", "held", "*_AXE", "amount", 5),
                Map.of("type", "REPUTATION", "key", "kingdom", "amount", "1000"),
                Map.of("type", "FISH")), false);

        assertEquals(3, criteria.size());
        assertEquals(TriggerType.KILL_ENTITY, criteria.get(0).type());
        assertEquals("*_AXE", criteria.get(0).held());
        assertEquals(5, criteria.get(0).amount());
        assertEquals("kingdom", criteria.get(1).key());
        assertEquals(1000, criteria.get(1).amount());
        assertEquals(1, criteria.get(2).amount());
    }

    @Test
    void parserRejectsUnknownTypesAndMissingType() {
        assertThrows(IllegalArgumentException.class,
                () -> CriterionParser.parseList("test", List.of(Map.of("type", "DANCE")), false));
        assertThrows(IllegalArgumentException.class,
                () -> CriterionParser.parseList("test", List.of(Map.of("amount", 3)), false));
    }

    @Test
    void reputationSourcesMustBeEvents() {
        assertThrows(IllegalArgumentException.class,
                () -> CriterionParser.parseList("test", List.of(Map.of("type", "REACH_LEVEL", "amount", 5)), true));
    }

}

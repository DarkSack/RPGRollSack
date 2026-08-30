package com.sack.rpgroll.api.race;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Race(null, "Elf", null, null, null, null, null, null));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Race("   ", "Elf", null, null, null, null, null, null));
    }

    @Test
    void constructorRejectsNullDisplayName() {
        assertThrows(NullPointerException.class,
                () -> new Race("elf", null, null, null, null, null, null, null));
    }

    @Test
    void constructorDefaultsNullDescriptionToEmptyString() {
        Race race = new Race("elf", "Elf", null, null, null, null, null, null);
        assertEquals("", race.description());
    }

    @Test
    void constructorDefaultsNullIconToEmptyString() {
        Race race = new Race("elf", "Elf", null, null, null, null, null, null);
        assertEquals("", race.icon());
    }

    @Test
    void constructorDefaultsNullCollectionsToEmpty() {
        Race race = new Race("elf", "Elf", null, null, null, null, null, null);

        assertTrue(race.baseAttributes().isEmpty());
        assertTrue(race.passiveTraits().isEmpty());
        assertTrue(race.lore().isEmpty());
    }

    @Test
    void constructorDefaultsNullPhysicalModifiersToNone() {
        Race race = new Race("elf", "Elf", null, null, null, null, null, null);
        assertEquals(RacePhysicalModifiers.none(), race.physicalModifiers());
    }

    @Test
    void constructorCopiesCollectionsDefensively() {
        var attributes = new java.util.HashMap<com.sack.rpgroll.api.stats.StatType, Integer>();
        attributes.put(com.sack.rpgroll.api.stats.StatType.DEXTERITY, 5);

        var traits = new java.util.ArrayList<>(List.of("trait-a"));

        Race race = new Race("elf", "Elf", "desc", attributes, traits, "icon", List.of("lore"),
                RacePhysicalModifiers.none());

        traits.add("trait-b");
        attributes.put(com.sack.rpgroll.api.stats.StatType.STRENGTH, 1);

        assertEquals(1, race.passiveTraits().size());
        assertEquals(1, race.baseAttributes().size());
    }

    @Test
    void constructorPreservesProvidedValues() {
        Map<com.sack.rpgroll.api.stats.StatType, Integer> attributes =
                Map.of(com.sack.rpgroll.api.stats.StatType.WISDOM, 3);

        Race race = new Race("elf", "Elf", "Graceful", attributes, List.of("keen-sight"), "base64icon",
                List.of("Line 1"), new RacePhysicalModifiers(1.1, 0.05, 2.0, 0.1));

        assertEquals("Graceful", race.description());
        assertEquals("base64icon", race.icon());
        assertEquals(3, race.baseAttributes().get(com.sack.rpgroll.api.stats.StatType.WISDOM));
        assertEquals(List.of("keen-sight"), race.passiveTraits());
        assertEquals(List.of("Line 1"), race.lore());
        assertEquals(1.1, race.physicalModifiers().scale());
    }
}

package com.sack.rpgroll.magic.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicSchoolTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new MagicSchool(null, "Fire", null, null, null, null, null, null, null));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        MagicSchool school = new MagicSchool("fire", "", null, null, null, null, null, null, null);
        assertEquals("fire", school.displayName());
    }

    @Test
    void blankColorDefaultsToWhite() {
        MagicSchool school = new MagicSchool("fire", "Fire", "  ", null, null, null, null, null, null);
        assertEquals("WHITE", school.color());
    }

    @Test
    void blankIconDefaultsToBook() {
        MagicSchool school = new MagicSchool("fire", "Fire", null, "", null, null, null, null, null);
        assertEquals("BOOK", school.icon());
    }

    @Test
    void blankCastSoundBecomesNull() {
        MagicSchool school = new MagicSchool("fire", "Fire", null, null, null, "   ", null, null, null);
        assertNull(school.castSoundOnCast());
    }

    @Test
    void blankCastEffectIdBecomesNull() {
        MagicSchool school = new MagicSchool("fire", "Fire", null, null, null, null, "", null, null);
        assertNull(school.castEffectId());
    }

    @Test
    void nullAffinityMapsBecomeEmpty() {
        MagicSchool school = new MagicSchool("fire", "Fire", null, null, null, null, null, null, null);

        assertTrue(school.raceAffinities().isEmpty());
        assertTrue(school.classAffinities().isEmpty());
    }

    @Test
    void affinityMapsAreCopiedDefensively() {
        Map<String, Double> raceAffinities = new HashMap<>();
        raceAffinities.put("elf", 0.25);

        MagicSchool school = new MagicSchool("fire", "Fire", null, null, null, null, null, raceAffinities, null);
        raceAffinities.put("dwarf", -0.1);

        assertEquals(1, school.raceAffinities().size());
        assertEquals(0.25, school.raceAffinities().get("elf"));
    }
}

package com.sack.rpgroll.magic.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrimoireTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Grimoire(null, "Book", null, null, null, 0, null));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        Grimoire grimoire = new Grimoire("fire-book", "  ", null, null, null, 0, null);
        assertEquals("fire-book", grimoire.displayName());
    }

    @Test
    void nullDisplayNameFallsBackToId() {
        Grimoire grimoire = new Grimoire("fire-book", null, null, null, null, 0, null);
        assertEquals("fire-book", grimoire.displayName());
    }

    @Test
    void blankIconDefaultsToWrittenBook() {
        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", "", null, null, 0, null);
        assertEquals("WRITTEN_BOOK", grimoire.icon());
    }

    @Test
    void nullDescriptionDefaultsToEmptyString() {
        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", null, null, null, 0, null);
        assertEquals("", grimoire.description());
    }

    @Test
    void blankSchoolIdBecomesNull() {
        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", null, null, "   ", 0, null);
        assertNull(grimoire.schoolId());
    }

    @Test
    void negativeRequiredLevelClampsToZero() {
        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", null, null, null, -5, null);
        assertEquals(0, grimoire.requiredLevel());
    }

    @Test
    void nullSpellIdsBecomesEmptyList() {
        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", null, null, null, 0, null);
        assertTrue(grimoire.spellIds().isEmpty());
    }

    @Test
    void spellIdsAreCopiedDefensively() {
        var mutable = new java.util.ArrayList<>(List.of("fireball"));

        Grimoire grimoire = new Grimoire("fire-book", "Fire Book", null, null, null, 0, mutable);
        mutable.add("firestorm");

        assertEquals(1, grimoire.spellIds().size());
    }
}

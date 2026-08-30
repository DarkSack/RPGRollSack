package com.sack.rpgroll.ascension.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityTest {

    @Test
    void nullIdIsRejected() {
        assertThrows(NullPointerException.class, () -> new Affinity(null, "Fuego", null, List.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankDisplayNameFallsBackToId(String displayName) {
        assertEquals("fire", new Affinity("fire", displayName, null, List.of()).displayName());
    }

    @Test
    void nullDisplayNameFallsBackToId() {
        assertEquals("fire", new Affinity("fire", null, null, List.of()).displayName());
    }

    @Test
    void explicitDisplayNameIsKept() {
        assertEquals("Fuego", new Affinity("fire", "Fuego", null, List.of()).displayName());
    }

    @Test
    void nullResistCausesIsNormalizedToEmpty() {
        assertTrue(new Affinity("fire", "Fuego", null, null).resistCauses().isEmpty());
    }

    @Test
    void resistCausesAreDefensivelyCopied() {
        List<String> causes = new ArrayList<>(List.of("FIRE"));
        Affinity affinity = new Affinity("fire", "Fuego", "water", causes);

        causes.add("MUTATED");

        assertEquals(List.of("FIRE"), affinity.resistCauses());
    }

    @Test
    void opposingIdIsPreservedAsGiven() {
        assertEquals("water", new Affinity("fire", "Fuego", "water", List.of()).opposingId());
    }
}

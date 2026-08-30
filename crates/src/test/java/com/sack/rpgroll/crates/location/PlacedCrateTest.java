package com.sack.rpgroll.crates.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlacedCrateTest {

    @Test
    void constructorRejectsNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> new PlacedCrate(null, "c", "world", 0, 0, 0));
        assertThrows(NullPointerException.class, () -> new PlacedCrate("p1", null, "world", 0, 0, 0));
        assertThrows(NullPointerException.class, () -> new PlacedCrate("p1", "c", null, 0, 0, 0));
    }

    @Test
    void hologramNameIsStableAndDerivedFromPlacementId() {
        PlacedCrate crate = new PlacedCrate("abc-123", "legendary", "world", 0, 0, 0);

        assertEquals("rpgroll-crate-abc-123", crate.hologramName());
    }
}

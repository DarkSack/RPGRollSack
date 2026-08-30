package com.sack.rpgroll.magic.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SpellCatalystTest {

    @Test
    void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new SpellCatalyst(null, "Wand", null, null, 1.0, 1.0, 1.0));
    }

    @Test
    void blankDisplayNameFallsBackToId() {
        SpellCatalyst catalyst = new SpellCatalyst("wand", " ", null, null, 1.0, 1.0, 1.0);
        assertEquals("wand", catalyst.displayName());
    }

    @Test
    void blankMaterialDefaultsToBlazeRod() {
        SpellCatalyst catalyst = new SpellCatalyst("wand", "Wand", "", null, 1.0, 1.0, 1.0);
        assertEquals("BLAZE_ROD", catalyst.material());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -0.001})
    void nonPositivePowerMultiplierClampsToOne(double value) {
        assertEquals(1.0, new SpellCatalyst("wand", "Wand", null, null, value, 1.0, 1.0).powerMultiplier());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0})
    void nonPositiveCostMultiplierClampsToOne(double value) {
        assertEquals(1.0, new SpellCatalyst("wand", "Wand", null, null, 1.0, value, 1.0).costMultiplier());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0})
    void nonPositiveRangeMultiplierClampsToOne(double value) {
        assertEquals(1.0, new SpellCatalyst("wand", "Wand", null, null, 1.0, 1.0, value).rangeMultiplier());
    }

    @Test
    void positiveMultipliersArePreserved() {
        SpellCatalyst catalyst = new SpellCatalyst("wand", "Wand", "BLAZE_ROD", "desc", 1.5, 0.8, 2.0);

        assertEquals(1.5, catalyst.powerMultiplier());
        assertEquals(0.8, catalyst.costMultiplier());
        assertEquals(2.0, catalyst.rangeMultiplier());
    }
}

package com.sack.rpgroll.api.race;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RacePhysicalModifiersTest {

    @Test
    void noneHasNeutralValuesForEveryField() {
        RacePhysicalModifiers none = RacePhysicalModifiers.none();

        assertEquals(1.0, none.scale());
        assertEquals(0.0, none.movementSpeedPercent());
        assertEquals(0.0, none.extraHealth());
        assertEquals(0.0, none.knockbackResistance());
    }

    @Test
    void noneHasNoModifiers() {
        assertFalse(RacePhysicalModifiers.none().hasAnyModifier());
    }

    @Test
    void hasAnyModifierTrueWhenScaleDiffers() {
        assertTrue(new RacePhysicalModifiers(1.5, 0.0, 0.0, 0.0).hasAnyModifier());
    }

    @Test
    void hasAnyModifierTrueWhenMovementSpeedDiffers() {
        assertTrue(new RacePhysicalModifiers(1.0, -0.15, 0.0, 0.0).hasAnyModifier());
    }

    @Test
    void hasAnyModifierTrueWhenExtraHealthDiffers() {
        assertTrue(new RacePhysicalModifiers(1.0, 0.0, 2.0, 0.0).hasAnyModifier());
    }

    @Test
    void hasAnyModifierTrueWhenKnockbackResistanceDiffers() {
        assertTrue(new RacePhysicalModifiers(1.0, 0.0, 0.0, 0.2).hasAnyModifier());
    }

    @Test
    void hasAnyModifierFalseWhenAllFieldsMatchNeutralValues() {
        assertFalse(new RacePhysicalModifiers(1.0, 0.0, 0.0, 0.0).hasAnyModifier());
    }
}

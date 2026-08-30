package com.sack.rpgroll.magic.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellCostTest {

    @Test
    void noneHasZeroCostsAndNoReagent() {
        SpellCost cost = SpellCost.none();

        assertEquals(0, cost.mana());
        assertEquals(0, cost.health());
        assertEquals(0, cost.experience());
        assertFalse(cost.hasReagent());
    }

    @Test
    void negativeManaClampsToZero() {
        assertEquals(0, new SpellCost(-5, 0, 0, null, 1).mana());
    }

    @Test
    void negativeHealthClampsToZero() {
        assertEquals(0, new SpellCost(0, -3, 0, null, 1).health());
    }

    @Test
    void negativeExperienceClampsToZero() {
        assertEquals(0, new SpellCost(0, 0, -10, null, 1).experience());
    }

    @Test
    void blankReagentMaterialBecomesNull() {
        assertNull(new SpellCost(0, 0, 0, "   ", 1).reagentMaterial());
    }

    @Test
    void emptyReagentMaterialBecomesNull() {
        assertNull(new SpellCost(0, 0, 0, "", 1).reagentMaterial());
    }

    @Test
    void nonBlankReagentMaterialIsPreserved() {
        assertEquals("DIAMOND", new SpellCost(0, 0, 0, "DIAMOND", 1).reagentMaterial());
    }

    @Test
    void reagentAmountBelowOneClampsToOne() {
        assertEquals(1, new SpellCost(0, 0, 0, "DIAMOND", 0).reagentAmount());
        assertEquals(1, new SpellCost(0, 0, 0, "DIAMOND", -5).reagentAmount());
    }

    @Test
    void hasReagentTrueOnlyWhenMaterialPresent() {
        assertTrue(new SpellCost(0, 0, 0, "DIAMOND", 1).hasReagent());
        assertFalse(new SpellCost(0, 0, 0, null, 1).hasReagent());
    }

    @Test
    void positiveValuesArePreservedUnchanged() {
        SpellCost cost = new SpellCost(10, 5, 20, "EMERALD", 3);

        assertEquals(10, cost.mana());
        assertEquals(5, cost.health());
        assertEquals(20, cost.experience());
        assertEquals("EMERALD", cost.reagentMaterial());
        assertEquals(3, cost.reagentAmount());
    }
}

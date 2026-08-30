package com.sack.rpgroll.items.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDurabilityDefTest {

    @Test
    void disabledHasZeroMaxDurabilityAndIsNotEnabled() {
        ItemDurabilityDef def = ItemDurabilityDef.disabled();

        assertFalse(def.enabled());
        assertFalse(def.repairable());
    }

    @Test
    void enabledIsTrueWhenMaxDurabilityPositive() {
        ItemDurabilityDef def = new ItemDurabilityDef(100, true, 1, 5);

        assertTrue(def.enabled());
    }

    @Test
    void enabledIsFalseWhenMaxDurabilityZeroOrNegative() {
        assertFalse(new ItemDurabilityDef(0, true, 1, 5).enabled());
        assertFalse(new ItemDurabilityDef(-10, true, 1, 5).enabled());
    }
}

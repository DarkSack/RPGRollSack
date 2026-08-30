package com.sack.rpgroll.items.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeLevelTest {

    @Test
    void constructorDefaultsNullStatBonusToEmptyMap() {
        UpgradeLevel level = new UpgradeLevel(1, null, null, null, 0, null, 0);

        assertTrue(level.statBonus().isEmpty());
    }

    @Test
    void constructorCopiesProvidedStatBonus() {
        UpgradeLevel level = new UpgradeLevel(2, Map.of("damage", 3.0), "Sharp Sword", "epic", 10.0, "DIAMOND", 5);

        assertEquals(3.0, level.statBonus().get("damage"));
        assertEquals("Sharp Sword", level.displayNameOverride());
        assertEquals("epic", level.rarityOverride());
    }
}

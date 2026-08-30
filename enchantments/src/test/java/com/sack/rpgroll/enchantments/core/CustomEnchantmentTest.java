package com.sack.rpgroll.enchantments.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomEnchantmentTest {

    private List<EnchantEffect> anyEffect() {
        return List.of(new EnchantEffect(EnchantEffectType.HEAL, Map.of()));
    }

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new CustomEnchantment("  ", "Name", null, null, null, 1, null, null, null, null, 0,
                        anyEffect()));
    }

    @Test
    void constructorRejectsMaxLevelBelowOne() {
        assertThrows(IllegalArgumentException.class,
                () -> new CustomEnchantment("sharpness", "Sharpness", null, null, null, 0, null, null, null, null, 0,
                        anyEffect()));
    }

    @Test
    void constructorRejectsNullOrEmptyEffects() {
        assertThrows(IllegalArgumentException.class,
                () -> new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null, null, null, null, 0,
                        null));
        assertThrows(IllegalArgumentException.class,
                () -> new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null, null, null, null, 0,
                        List.of()));
    }

    @Test
    void constructorDefaultsRarityToCommonWhenNull() {
        CustomEnchantment enchantment = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null,
                null, null, null, 0, anyEffect());

        assertEquals(Rarity.COMMON, enchantment.rarity());
    }

    @Test
    void constructorDefaultsChanceTo100WhenZeroOrNegative() {
        CustomEnchantment atZero = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null, null,
                null, null, 0, anyEffect());
        CustomEnchantment negative = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null, null,
                null, null, -5, anyEffect());

        assertEquals(100.0, atZero.chance());
        assertEquals(100.0, negative.chance());
    }

    @Test
    void constructorPreservesPositiveChance() {
        CustomEnchantment enchantment = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 1, null,
                null, null, null, 25.0, anyEffect());

        assertEquals(25.0, enchantment.chance());
    }

    @Test
    void levelDataReturnsEmptyMapWhenLevelNotConfigured() {
        CustomEnchantment enchantment = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 3,
                Map.of(1, Map.of("damage", 2.0)), null, null, null, 0, anyEffect());

        assertTrue(enchantment.levelData(2).isEmpty());
    }

    @Test
    void levelDataReturnsConfiguredDataForLevel() {
        CustomEnchantment enchantment = new CustomEnchantment("sharpness", "Sharpness", null, null, null, 3,
                Map.of(1, Map.of("damage", 2.0), 2, Map.of("damage", 4.0)), null, null, null, 0, anyEffect());

        assertEquals(4.0, enchantment.levelData(2).get("damage"));
    }
}

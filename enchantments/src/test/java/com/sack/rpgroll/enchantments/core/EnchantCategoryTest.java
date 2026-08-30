package com.sack.rpgroll.enchantments.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantCategoryTest {

    @ParameterizedTest
    @EnumSource(EnchantCategory.class)
    void anyMatchesEveryMaterial(EnchantCategory category) {
        if (category == EnchantCategory.ANY) {
            assertTrue(category.matches(Material.DIRT));
        }
    }

    @Test
    void weaponMatchesSwordsAxesAndTrident() {
        assertTrue(EnchantCategory.WEAPON.matches(Material.DIAMOND_SWORD));
        assertTrue(EnchantCategory.WEAPON.matches(Material.IRON_AXE));
        assertTrue(EnchantCategory.WEAPON.matches(Material.TRIDENT));
        assertFalse(EnchantCategory.WEAPON.matches(Material.DIAMOND_PICKAXE));
    }

    @Test
    void armorMatchesAllArmorPiecesPlusElytraAndTurtleHelmet() {
        assertTrue(EnchantCategory.ARMOR.matches(Material.IRON_HELMET));
        assertTrue(EnchantCategory.ARMOR.matches(Material.IRON_CHESTPLATE));
        assertTrue(EnchantCategory.ARMOR.matches(Material.IRON_LEGGINGS));
        assertTrue(EnchantCategory.ARMOR.matches(Material.IRON_BOOTS));
        assertTrue(EnchantCategory.ARMOR.matches(Material.ELYTRA));
        assertTrue(EnchantCategory.ARMOR.matches(Material.TURTLE_HELMET));
        assertFalse(EnchantCategory.ARMOR.matches(Material.DIAMOND_SWORD));
    }

    @Test
    void toolsMatchesPickaxeShovelHoeAndAxe() {
        assertTrue(EnchantCategory.TOOLS.matches(Material.DIAMOND_PICKAXE));
        assertTrue(EnchantCategory.TOOLS.matches(Material.DIAMOND_SHOVEL));
        assertTrue(EnchantCategory.TOOLS.matches(Material.DIAMOND_HOE));
        assertTrue(EnchantCategory.TOOLS.matches(Material.DIAMOND_AXE));
        assertFalse(EnchantCategory.TOOLS.matches(Material.DIAMOND_SWORD));
    }

    @Test
    void specificSlotCategoriesOnlyMatchTheirOwnPiece() {
        assertTrue(EnchantCategory.HELMET.matches(Material.IRON_HELMET));
        assertFalse(EnchantCategory.HELMET.matches(Material.IRON_CHESTPLATE));
        assertTrue(EnchantCategory.BOW.matches(Material.BOW));
        assertFalse(EnchantCategory.BOW.matches(Material.CROSSBOW));
    }

    @Test
    void fromStringParsesCaseInsensitivelyAndTrims() {
        assertTrue(EnchantCategory.fromString(" weapon ") == EnchantCategory.WEAPON);
    }

    @Test
    void fromStringReturnsNullForUnknownValue() {
        assertNull(EnchantCategory.fromString("not-a-category"));
    }
}

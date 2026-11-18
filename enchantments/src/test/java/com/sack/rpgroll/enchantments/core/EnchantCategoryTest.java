package com.sack.rpgroll.enchantments.core;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantCategoryTest {

    @Test
    void weaponCoversMaceSpearsAndTrident() {
        assertTrue(EnchantCategory.WEAPON.matches(Material.MACE));
        assertTrue(EnchantCategory.WEAPON.matches(Material.valueOf("NETHERITE_SPEAR")));
        assertTrue(EnchantCategory.WEAPON.matches(Material.TRIDENT));
        assertTrue(EnchantCategory.SPEAR.matches(Material.valueOf("WOODEN_SPEAR")));
        assertFalse(EnchantCategory.WEAPON.matches(Material.BREEZE_ROD));
    }

    @Test
    void toolsCoverShears() {
        assertTrue(EnchantCategory.TOOLS.matches(Material.SHEARS));
        assertFalse(EnchantCategory.TOOLS.matches(Material.SHIELD));
    }
}

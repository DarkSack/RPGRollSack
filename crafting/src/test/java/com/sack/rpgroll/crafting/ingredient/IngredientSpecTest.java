package com.sack.rpgroll.crafting.ingredient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngredientSpecTest {

    @Test
    void constructorClampsAmountToAtLeastOne() {
        assertEquals(1, new IngredientSpec(IngredientType.MATERIAL, "STONE", 0, null).amount());
        assertEquals(1, new IngredientSpec(IngredientType.MATERIAL, "STONE", -5, null).amount());
        assertEquals(3, new IngredientSpec(IngredientType.MATERIAL, "STONE", 3, null).amount());
    }

    @Test
    void materialFactoryCreatesMaterialTypeWithNoMinQuality() {
        IngredientSpec spec = IngredientSpec.material("IRON_INGOT", 2);

        assertEquals(IngredientType.MATERIAL, spec.type());
        assertEquals("IRON_INGOT", spec.value());
        assertFalse(spec.hasMinQuality());
    }

    @Test
    void tagFactoryCreatesTagType() {
        IngredientSpec spec = IngredientSpec.tag("logs", 4);

        assertEquals(IngredientType.TAG, spec.type());
    }

    @Test
    void itemIdFactoryCreatesItemIdType() {
        IngredientSpec spec = IngredientSpec.itemId("magic_sword", 1);

        assertEquals(IngredientType.ITEM_ID, spec.type());
    }

    @Test
    void hasMinQualityFalseWhenNullOrBlank() {
        assertFalse(new IngredientSpec(IngredientType.MATERIAL, "STONE", 1, null).hasMinQuality());
        assertFalse(new IngredientSpec(IngredientType.MATERIAL, "STONE", 1, "  ").hasMinQuality());
    }

    @Test
    void hasMinQualityTrueWhenSet() {
        assertTrue(new IngredientSpec(IngredientType.MATERIAL, "STONE", 1, "fine").hasMinQuality());
    }
}

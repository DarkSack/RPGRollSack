package com.sack.rpgroll.crafting.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomRecipeTest {

    private RecipeResult anyResult() {
        return new RecipeResult(RecipeResultType.MATERIAL, "DIAMOND", 1);
    }

    private CustomRecipe recipeWith(String displayName, String icon, int processingTimeTicks, int fuelPerCraft,
            double xpAmount, double economyCost) {
        return new CustomRecipe("smelt-iron", displayName, icon, "forge", null, anyResult(), null,
                processingTimeTicks, fuelPerCraft, xpAmount, null, economyCost, 0, false);
    }

    @Test
    void constructorRejectsNullIdStationIdOrResult() {
        assertThrows(NullPointerException.class,
                () -> new CustomRecipe(null, null, null, "forge", null, anyResult(), null, 0, 0, 0, null, 0, 0,
                        false));
        assertThrows(NullPointerException.class,
                () -> new CustomRecipe("smelt-iron", null, null, null, null, anyResult(), null, 0, 0, 0, null, 0, 0,
                        false));
        assertThrows(NullPointerException.class,
                () -> new CustomRecipe("smelt-iron", null, null, "forge", null, null, null, 0, 0, 0, null, 0, 0,
                        false));
    }

    @Test
    void constructorDefaultsDisplayNameAndIconWhenBlank() {
        CustomRecipe recipe = recipeWith(null, null, 0, 0, 0, 0);

        assertEquals("smelt-iron", recipe.displayName());
        assertEquals("CRAFTING_TABLE", recipe.icon());
    }

    @Test
    void constructorPreservesProvidedDisplayNameAndIcon() {
        CustomRecipe recipe = recipeWith("Smelt Iron", "FURNACE", 0, 0, 0, 0);

        assertEquals("Smelt Iron", recipe.displayName());
        assertEquals("FURNACE", recipe.icon());
    }

    @Test
    void constructorClampsNegativeNumericFieldsToZero() {
        CustomRecipe recipe = recipeWith(null, null, -10, -5, -3, -100);

        assertEquals(0, recipe.processingTimeTicks());
        assertEquals(0, recipe.fuelPerCraft());
        assertEquals(0.0, recipe.xpAmount());
        assertEquals(0.0, recipe.economyCost());
    }

    @Test
    void constructorDefaultsNullListsToEmpty() {
        CustomRecipe recipe = recipeWith(null, null, 0, 0, 0, 0);

        assertTrue(recipe.ingredients().isEmpty());
        assertTrue(recipe.conditions().isEmpty());
    }
}

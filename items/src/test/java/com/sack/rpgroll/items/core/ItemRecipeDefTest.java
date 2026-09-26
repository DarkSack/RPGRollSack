package com.sack.rpgroll.items.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ItemRecipeDefTest {

    @Test
    void amountDefaultsToOneAndIsClamped() {
        assertEquals(1, new ItemRecipeDef(RecipeType.SHAPELESS, List.of(), Map.of(), List.of("DIRT"), null, 200, null).amount());
        assertEquals(9, new ItemRecipeDef(RecipeType.SHAPELESS, List.of(), Map.of(), List.of("DIRT"), null, 200, null, 9).amount());
        assertEquals(1, new ItemRecipeDef(RecipeType.SHAPELESS, List.of(), Map.of(), List.of(), null, 200, null, 0).amount());
        assertEquals(64, new ItemRecipeDef(RecipeType.SHAPELESS, List.of(), Map.of(), List.of(), null, 200, null, 500).amount());
    }

    @Test
    void furnaceInputFallsBackToBaseMaterial() {
        assertEquals("item:mitrilo_en_bruto", new ItemRecipeDef(RecipeType.FURNACE, null, null,
                List.of("item:mitrilo_en_bruto"), "IRON_ORE", 200, null).singleInput());
        assertEquals("IRON_SWORD", new ItemRecipeDef(RecipeType.FURNACE, null, null, null, "IRON_SWORD", 200, null)
                .singleInput());
        assertNull(new ItemRecipeDef(RecipeType.FURNACE, null, null, null, null, 200, null).singleInput());
    }

}

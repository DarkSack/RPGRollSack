package com.sack.rpgroll.crafting.recipe;

public record RecipeResult(RecipeResultType type, String value, int amount) {

    public RecipeResult {
        amount = Math.max(1, amount);
    }

}

package com.sack.rpgroll.extras.backpack;

import java.util.List;
import java.util.Map;

/** Receta con forma de un nivel de mochila (hasta 3x3). */
public record BackpackRecipe(List<String> shape, Map<Character, BackpackIngredient> ingredients) {

    public boolean usesBackpack() {
        return ingredients.values().stream().anyMatch(i -> i.kind() == BackpackIngredient.Kind.BACKPACK);
    }

}

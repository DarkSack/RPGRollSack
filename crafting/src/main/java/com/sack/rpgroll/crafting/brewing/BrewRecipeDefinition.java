package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Receta de fermentación personalizada: si el ingrediente puesto en el
 * soporte superior de la estación de pociones cumple {@code ingredient}, el
 * resultado vanilla de esa tanda se reemplaza por {@code result} en cada
 * botella no vacía. Bukkit no tiene un {@code Recipe} de fermentación —
 * {@code BrewingEngine} escucha {@code BrewEvent} directamente.
 */
public record BrewRecipeDefinition(
        String id,
        IngredientSpec ingredient,
        RecipeResult result,
        List<RecipeCondition> conditions) implements RPGContent {

    public BrewRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(ingredient, "ingredient no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

}

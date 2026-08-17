package com.sack.rpgroll.crafting.grindstone;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Combinación personalizada de piedra de amolar: si el slot superior cumple
 * {@code upperIngredient} y el inferior {@code lowerIngredient}, el
 * resultado vanilla (quitar encantamientos) se reemplaza por {@code result}.
 * Bukkit expone {@code PrepareGrindstoneEvent} para esto — ver {@code GrindstoneEngine}.
 */
public record GrindstoneRecipeDefinition(
        String id,
        IngredientSpec upperIngredient,
        IngredientSpec lowerIngredient,
        RecipeResult result,
        List<RecipeCondition> conditions) implements RPGContent {

    public GrindstoneRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(upperIngredient, "upperIngredient no puede ser null");
        Objects.requireNonNull(lowerIngredient, "lowerIngredient no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

}

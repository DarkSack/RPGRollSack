package com.sack.rpgroll.crafting.anvil;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Combinación personalizada de yunque: si el slot izquierdo cumple
 * {@code baseIngredient} y el derecho {@code additionIngredient}, el
 * resultado vanilla (reparación/renombrado normal) se reemplaza por
 * {@code result}. Bukkit no expone un {@code Recipe} para el yunque —
 * {@code AnvilEngine} escucha {@code PrepareAnvilEvent} directamente.
 */
public record AnvilRecipeDefinition(
        String id,
        IngredientSpec baseIngredient,
        IngredientSpec additionIngredient,
        RecipeResult result,
        int repairCostLevels,
        List<RecipeCondition> conditions) implements RPGContent {

    public AnvilRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(baseIngredient, "baseIngredient no puede ser null");
        Objects.requireNonNull(additionIngredient, "additionIngredient no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        repairCostLevels = Math.max(0, repairCostLevels);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

}

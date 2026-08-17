package com.sack.rpgroll.crafting.cartography;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Combinación personalizada de mesa de cartografía: si el slot del mapa
 * cumple {@code mapIngredient} y el slot del ítem {@code itemIngredient}, el
 * resultado se reemplaza por {@code result}. Bukkit no expone un
 * {@code PrepareX} genérico para esta mesa — {@code CartographyEngine}
 * escucha {@code CartographyItemEvent} (Paper) y sobreescribe el resultado
 * vía {@code CartographyInventory#setResult}.
 */
public record CartographyRecipeDefinition(
        String id,
        IngredientSpec mapIngredient,
        IngredientSpec itemIngredient,
        RecipeResult result,
        List<RecipeCondition> conditions) implements RPGContent {

    public CartographyRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(mapIngredient, "mapIngredient no puede ser null");
        Objects.requireNonNull(itemIngredient, "itemIngredient no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

}

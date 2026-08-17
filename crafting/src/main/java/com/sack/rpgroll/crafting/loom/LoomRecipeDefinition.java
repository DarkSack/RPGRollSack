package com.sack.rpgroll.crafting.loom;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Combinación personalizada de telar: banner + tinte (+ opcionalmente un
 * ítem de patrón concreto) producen {@code result} en vez del resultado
 * vanilla. Bukkit no expone {@code LoomInventory} con getters de slot ni un
 * {@code PrepareLoomEvent} — {@code LoomEngine} intercepta el click en el
 * slot de salida directamente (ver esa clase).
 *
 * @param patternIngredient opcional (null = no exige un ítem de patrón puntual, solo banner+tinte)
 */
public record LoomRecipeDefinition(
        String id,
        IngredientSpec bannerIngredient,
        IngredientSpec dyeIngredient,
        IngredientSpec patternIngredient,
        RecipeResult result,
        List<RecipeCondition> conditions) implements RPGContent {

    public LoomRecipeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(bannerIngredient, "bannerIngredient no puede ser null");
        Objects.requireNonNull(dyeIngredient, "dyeIngredient no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public boolean hasPatternIngredient() {
        return patternIngredient != null;
    }

}

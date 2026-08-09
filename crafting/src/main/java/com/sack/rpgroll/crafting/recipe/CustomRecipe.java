package com.sack.rpgroll.crafting.recipe;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;

import java.util.List;
import java.util.Objects;

/**
 * Receta de una {@code CustomStation}: qué ingredientes consume, qué produce,
 * cuánto tarda y qué debe cumplir el jugador para poder iniciarla. Las
 * recetas de estaciones vanilla (mesa de crafteo, hornos, cortadora, mesa de
 * herrería) se registran directamente en Bukkit y no usan este tipo — ver
 * {@code VanillaRecipeBridge}.
 *
 * @param stationId          id de la {@code CustomStation} donde se procesa esta receta
 * @param processingTimeTicks cuánto tarda una vez iniciada
 * @param fuelPerCraft        unidades de combustible consumidas por crafteo completo (0 = no requiere)
 * @param xpAmount            xp de personaje (RPGPlayer, vía RPGRollAPI) otorgada al completar (0 = ninguna)
 * @param economyCurrencyId   id de moneda de RPGRoll-Economy a cobrar (null = usa la moneda base)
 * @param economyCost         costo monetario al iniciar el crafteo (0 = gratis)
 * @param failChance          probabilidad de fallo (0.0-1.0); negativo = usar default de config.yml
 * @param qualityEnabled      si el resultado pasa por el sistema de calidad al completarse
 */
public record CustomRecipe(
        String id,
        String displayName,
        String icon,
        String stationId,
        List<IngredientSpec> ingredients,
        RecipeResult result,
        List<RecipeCondition> conditions,
        int processingTimeTicks,
        int fuelPerCraft,
        double xpAmount,
        String economyCurrencyId,
        double economyCost,
        double failChance,
        boolean qualityEnabled) implements RPGContent {

    public CustomRecipe {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(stationId, "stationId no puede ser null");
        Objects.requireNonNull(result, "result no puede ser null");

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "CRAFTING_TABLE" : icon;
        ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        processingTimeTicks = Math.max(0, processingTimeTicks);
        fuelPerCraft = Math.max(0, fuelPerCraft);
        xpAmount = Math.max(0, xpAmount);
        economyCost = Math.max(0, economyCost);
    }

}

package com.sack.rpgroll.crafting.villager;

import com.sack.rpgroll.common.content.RPGContent;
import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;

import java.util.List;
import java.util.Objects;

/**
 * Un comercio personalizado que un aldeano "vinculado" (ver
 * {@code VillagerTradeEngine} y {@code /craftingadmin villager bind}) puede
 * ofrecer en vez de sus comercios vanilla normales. Los costos se
 * construyen con {@code RecipeResultFactory} igual que un resultado — a
 * diferencia de una receta de estación, acá no "matchean" un ingrediente
 * puesto por el jugador, son el precio fijo que Bukkit muestra en la GUI de
 * comercio.
 *
 * @param costs               1 o 2 costos (igual que un {@code MerchantRecipe} vanilla)
 * @param maxUses             usos antes de que el comercio se agote hasta refrescar
 * @param villagerExperience  xp de aldeano otorgada al aldeano por cada uso
 * @param rewardsExperience   si el jugador recibe xp vanilla al comerciar
 */
public record VillagerTradeDefinition(
        String id,
        String displayName,
        String icon,
        List<RecipeResult> costs,
        RecipeResult result,
        int maxUses,
        int villagerExperience,
        boolean rewardsExperience,
        List<RecipeCondition> conditions,
        double xpAmount,
        String economyCurrencyId,
        double economyCost,
        boolean qualityEnabled) implements RPGContent {

    public VillagerTradeDefinition {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "EMERALD" : icon;
        Objects.requireNonNull(result, "result no puede ser null");
        costs = costs == null || costs.isEmpty() ? List.of(new RecipeResult(
                com.sack.rpgroll.crafting.recipe.RecipeResultType.MATERIAL, "EMERALD", 1)) : List.copyOf(costs).subList(0,
                Math.min(2, costs.size()));
        maxUses = Math.max(1, maxUses);
        villagerExperience = Math.max(0, villagerExperience);
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        economyCost = Math.max(0, economyCost);
        xpAmount = Math.max(0, xpAmount);
    }

}

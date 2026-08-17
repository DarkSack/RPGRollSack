package com.sack.rpgroll.crafting.station.tier;

import com.sack.rpgroll.crafting.ingredient.IngredientSpec;

import java.util.List;

/**
 * Costo para subir una {@code CustomStation} de {@code tier - 1} a {@code tier}.
 *
 * @param tier              el nivel al que se sube (2, 3, 4...) — nunca 1, ese es el nivel base gratis
 * @param cost              ingredientes a consumir del inventario del jugador
 * @param economyCost       costo monetario adicional (0 = gratis)
 * @param economyCurrencyId moneda de RPGRoll-Economy a cobrar (null = usa la moneda base)
 */
public record TierUpgrade(int tier, List<IngredientSpec> cost, double economyCost, String economyCurrencyId) {

    public TierUpgrade {
        tier = Math.max(2, tier);
        cost = cost == null ? List.of() : List.copyOf(cost);
        economyCost = Math.max(0, economyCost);
    }

}

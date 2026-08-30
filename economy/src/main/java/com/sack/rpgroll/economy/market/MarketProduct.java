package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;
import java.util.Objects;

/**
 * Define cómo reacciona el precio de un producto a la oferta y la demanda —
 * el {@code id} típicamente es un {@code Material} vanilla en mayúsculas
 * (ej. {@code IRON_INGOT}) pero puede ser cualquier clave lógica que otro
 * addon quiera cotizar (una especie de pez, un ítem de RPGRoll-Items, etc.).
 */
public record MarketProduct(
        String id,
        String displayName,
        String icon,
        String currencyId,
        double basePrice,
        double minPrice,
        double maxPrice,
        double supplyWeight,
        double demandWeight,
        double volatility,
        double recoveryRate,
        String category,
        Map<String, Double> seasonTagModifiers) implements RPGContent {

    public MarketProduct {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "PAPER" : icon;
        currencyId = currencyId == null || currencyId.isBlank() ? null : currencyId;
        basePrice = basePrice <= 0 ? 1.0 : basePrice;
        minPrice = minPrice <= 0 ? basePrice * 0.1 : minPrice;
        maxPrice = maxPrice <= 0 ? basePrice * 10 : maxPrice;
        supplyWeight = supplyWeight <= 0 ? 1.0 : supplyWeight;
        demandWeight = demandWeight <= 0 ? 1.0 : demandWeight;
        volatility = volatility <= 0 ? 0.1 : volatility;
        recoveryRate = recoveryRate <= 0 ? 0.02 : recoveryRate;
        category = category == null || category.isBlank() ? "misc" : category;
        seasonTagModifiers = seasonTagModifiers == null ? Map.of() : Map.copyOf(seasonTagModifiers);
    }

}

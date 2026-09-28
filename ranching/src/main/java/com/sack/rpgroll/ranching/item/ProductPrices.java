package com.sack.rpgroll.ranching.item;

import com.sack.rpgroll.ranching.core.production.ProductQuality;

import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lo que vale cada producto para el comprador de RPGRoll-Economy: {@code product-prices} (por unidad,
 * según el tipo) por {@code product-quality-multipliers} (según la calidad). Se lee del config.yml al
 * arrancar y en cada recarga.
 */
public final class ProductPrices {

    private static final Map<String, Double> PRICES = new HashMap<>();
    private static final Map<ProductQuality, Double> MULTIPLIERS = new EnumMap<>(ProductQuality.class);

    private ProductPrices() {
    }

    public static void load(ConfigurationSection prices, ConfigurationSection multipliers) {

        PRICES.clear();
        MULTIPLIERS.clear();

        if (prices != null) {
            for (String type : prices.getKeys(false)) {
                PRICES.put(type.toLowerCase(Locale.ROOT), Math.max(0, prices.getDouble(type)));
            }
        }

        if (multipliers != null) {
            for (ProductQuality quality : ProductQuality.values()) {
                MULTIPLIERS.put(quality, Math.max(0, multipliers.getDouble(quality.name(), 1.0)));
            }
        }
    }

    /** Valor de una unidad; 0 si el tipo no tiene precio (el comprador no lo acepta). */
    public static double valueOf(String productType, ProductQuality quality) {
        double base = PRICES.getOrDefault(productType.toLowerCase(Locale.ROOT), 0.0);
        return base * (quality == null ? 1.0 : MULTIPLIERS.getOrDefault(quality, 1.0));
    }

}

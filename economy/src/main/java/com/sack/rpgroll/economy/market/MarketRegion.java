package com.sack.rpgroll.economy.market;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;
import java.util.Objects;

/**
 * Una caja (AABB, sin WorldGuard — mismo estilo simple que {@code SeasonRegion}/
 * {@code FishingRegion}) que ajusta el precio de mercado dentro de sus límites.
 * Dos jugadores comprando el mismo producto en dos regiones distintas pueden
 * ver precios distintos: una ciudad minera puede tener el mineral más barato
 * ({@code categoryModifiers: {mineral: 0.7}}) mientras que una capital lo
 * tiene más caro por escasez local. Sin ninguna región definida, o fuera de
 * todas, el precio es el global de siempre (multiplicador implícito 1.0).
 */
public record MarketRegion(String id, String displayName, String world, double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ, Map<String, Double> categoryModifiers,
        Map<String, Double> productModifiers) implements RPGContent {

    public MarketRegion {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        categoryModifiers = categoryModifiers == null ? Map.of() : Map.copyOf(categoryModifiers);
        productModifiers = productModifiers == null ? Map.of() : Map.copyOf(productModifiers);
    }

    public boolean contains(String worldName, double x, double y, double z) {
        return world.equalsIgnoreCase(worldName)
                && x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                && y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
                && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
    }

    /** El modificador de un producto: su propio id si está definido, si no el de su categoría, si no 1.0 (sin efecto). */
    public double modifierFor(MarketProduct product) {
        Double byProduct = productModifiers.get(product.id());
        if (byProduct != null) {
            return byProduct;
        }
        return categoryModifiers.getOrDefault(product.category(), 1.0);
    }

}

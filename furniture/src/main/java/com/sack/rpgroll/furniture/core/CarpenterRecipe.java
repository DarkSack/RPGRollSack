package com.sack.rpgroll.furniture.core;

import org.bukkit.Material;

import java.util.Map;

/**
 * Lo que cuesta fabricar un mueble en un carpintero.
 *
 * @param station   qué carpintero lo fabrica ({@value #DEFAULT_STATION} por defecto)
 * @param materials materiales vanilla y cuántos de cada uno
 * @param money     dinero (Vault); 0 = gratis
 * @param amount    cuántos salen
 */
public record CarpenterRecipe(String station, Map<Material, Integer> materials, double money, int amount) {

    public static final String DEFAULT_STATION = "carpenter";

    public CarpenterRecipe {
        materials = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(materials));
        amount = Math.max(1, amount);
        money = Math.max(0, money);
    }
}

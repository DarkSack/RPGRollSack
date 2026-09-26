package com.sack.rpgroll.extras.backpack;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Un ingrediente de receta de mochila: un material vanilla, un ítem de
 * RPGRoll-Items ({@code item:<id>}) o la mochila del nivel anterior
 * ({@code backpack}).
 */
public record BackpackIngredient(Kind kind, Material material, String itemId) {

    public enum Kind {
        MATERIAL, ITEM, BACKPACK
    }

    public static final BackpackIngredient BACKPACK = new BackpackIngredient(Kind.BACKPACK, Material.PLAYER_HEAD, null);

    /** @throws IllegalArgumentException si la línea no es válida */
    public static BackpackIngredient parse(String raw) {

        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("ingrediente vacío");
        }

        String value = raw.trim();

        if (value.equalsIgnoreCase("backpack") || value.equalsIgnoreCase("mochila")) {
            return BACKPACK;
        }

        if (value.regionMatches(true, 0, "item:", 0, 5)) {
            String id = value.substring(5).trim();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("falta el id del ítem en «" + raw + "»");
            }
            // El material real se conoce al registrar la receta, cuando RPGRoll-Items ya cargó.
            return new BackpackIngredient(Kind.ITEM, null, id);
        }

        Material material;
        try {
            material = Material.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("material desconocido «" + raw + "»");
        }

        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR
                || material.name().startsWith("LEGACY_")) {
            throw new IllegalArgumentException("material no válido «" + raw + "»");
        }

        return new BackpackIngredient(Kind.MATERIAL, material, null);
    }

}

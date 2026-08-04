package com.sack.rpgroll.enchantments.core;

import org.bukkit.Material;

import java.util.Locale;

/**
 * Categoría de ítem a la que puede aplicarse un encantamiento. Se evalúa
 * contra el {@link Material} del ítem — no requiere mantener listas de
 * materiales a mano salvo que el YAML use "allowed-items" para restringir
 * a materiales específicos en vez de una categoría completa.
 */
public enum EnchantCategory {

    WEAPON,
    ARMOR,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    TOOLS,
    BOW,
    CROSSBOW,
    TRIDENT,
    FISHING_ROD,
    ANY;

    public boolean matches(Material material) {

        if (this == ANY) {
            return true;
        }

        String name = material.name();

        return switch (this) {
            case WEAPON -> name.endsWith("_SWORD") || name.endsWith("_AXE") || material == Material.TRIDENT;
            case ARMOR -> name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                    || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                    || material == Material.TURTLE_HELMET || material == Material.ELYTRA;
            case HELMET -> name.endsWith("_HELMET") || material == Material.TURTLE_HELMET;
            case CHESTPLATE -> name.endsWith("_CHESTPLATE") || material == Material.ELYTRA;
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
            case TOOLS -> name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL")
                    || name.endsWith("_HOE") || name.endsWith("_AXE");
            case BOW -> material == Material.BOW;
            case CROSSBOW -> material == Material.CROSSBOW;
            case TRIDENT -> material == Material.TRIDENT;
            case FISHING_ROD -> material == Material.FISHING_ROD;
            default -> false;
        };
    }

    public static EnchantCategory fromString(String raw) {
        try {
            return EnchantCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

package com.sack.rpgroll.gameplay.enchant;

import org.bukkit.Material;

import java.util.Optional;

/**
 * Categoría general de un item, usada para validar a qué tipo de equipo
 * puede aplicarse un encantamiento custom.
 * <p>
 * Nota: las hachas (_AXE) se clasifican como TOOL, no WEAPON, a pesar de
 * poder usarse en combate — simplificación consciente para esta fase.
 */
public enum ItemCategory {
    WEAPON,
    ARMOR,
    TOOL;

    public static Optional<ItemCategory> fromMaterial(Material material) {

        String name = material.name();

        if (name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT")) {
            return Optional.of(WEAPON);
        }

        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS") || name.equals("ELYTRA") || name.equals("TURTLE_HELMET")) {
            return Optional.of(ARMOR);
        }

        if (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")) {
            return Optional.of(TOOL);
        }

        return Optional.empty();
    }
}
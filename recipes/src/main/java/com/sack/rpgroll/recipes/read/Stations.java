package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.recipe.RecipeStation;

import org.bukkit.Material;

import java.util.Locale;

/** Estaciones vanilla por id, para las recetas de {@code extra/}. */
public final class Stations {

    private Stations() {
    }

    public static RecipeStation vanilla(String id) {
        String clean = id.toLowerCase(Locale.ROOT);
        Material icon = switch (clean) {
            case RecipeStation.VILLAGER -> Material.EMERALD;
            case RecipeStation.CAMPFIRE -> Material.CAMPFIRE;
            default -> {
                Material material = Material.matchMaterial(clean);
                yield material != null && material.isItem() ? material : Material.CRAFTING_TABLE;
            }
        };
        return RecipeStation.vanilla(clean, icon);
    }
}

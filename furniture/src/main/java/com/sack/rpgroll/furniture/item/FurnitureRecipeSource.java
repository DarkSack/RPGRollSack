package com.sack.rpgroll.furniture.item;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSource;
import com.sack.rpgroll.common.recipe.RecipeStation;
import com.sack.rpgroll.furniture.core.CarpenterRecipe;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureManager;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Lo que fabrica cada carpintero, para el recetario (RPGRoll-Recipes). */
public final class FurnitureRecipeSource implements RecipeSource {

    private final FurnitureManager manager;
    private final FurnitureItems items;
    private final LangManager lang;

    public FurnitureRecipeSource(FurnitureManager manager, FurnitureItems items, LangManager lang) {
        this.manager = manager;
        this.items = items;
        this.lang = lang;
    }

    @Override
    public String name() {
        return "RPGRoll-Furniture";
    }

    @Override
    public Collection<RecipeEntry> recipes() {

        List<RecipeEntry> entries = new ArrayList<>();

        for (FurnitureDefinition def : manager.all()) {
            if (def.variants().isEmpty()) {
                if (def.recipe() != null) {
                    entries.add(entry(def, null, def.recipe()));
                }
                continue;
            }
            for (String variant : def.variants().keySet()) {
                def.recipe(variant).ifPresent(recipe -> entries.add(entry(def, variant, recipe)));
            }
        }

        return entries;
    }

    private RecipeEntry entry(FurnitureDefinition def, String variant, CarpenterRecipe recipe) {

        String stationKey = "station." + recipe.station();
        String stationName = lang.raw(stationKey);
        RecipeStation station = new RecipeStation("rpgroll-furniture:" + recipe.station(),
                stationName.equals(stationKey) ? recipe.station() : stationName, ItemStack.of(icon(recipe.station())));

        RecipeEntry.Builder builder = RecipeEntry.builder(def.id() + (variant == null ? "" : "/" + variant), station);
        for (Map.Entry<Material, Integer> material : recipe.materials().entrySet()) {
            builder.input(ItemStack.of(material.getKey(), Math.max(1, Math.min(99, material.getValue()))));
        }
        builder.output(items.create(def, variant, recipe.amount()));
        if (recipe.money() > 0) {
            builder.note(lang.raw("recipe_viewer.money", "amount",
                    new java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT))
                            .format(recipe.money())));
        }
        if (def.permission() != null && !def.permission().isBlank()) {
            String permission = def.permission();
            builder.visibleTo(player -> player.hasPermission(permission));
        }
        return builder.build();
    }

    private static Material icon(String station) {
        return switch (station) {
            case "blacksmith" -> Material.ANVIL;
            case "tailor" -> Material.LOOM;
            case "potter" -> Material.DECORATED_POT;
            default -> Material.FLETCHING_TABLE;
        };
    }
}

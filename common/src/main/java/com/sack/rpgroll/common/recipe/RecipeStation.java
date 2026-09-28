package com.sack.rpgroll.common.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Dónde se hace una receta.
 *
 * @param id   id estable ({@code crafting_table}, {@code furnace}, {@code rpgroll-crafting:forja}...);
 *             las estaciones con el mismo id se agrupan en el visor
 * @param name nombre para mostrar (legacy {@code &} o MiniMessage); null = el visor usa su
 *             traducción para las estaciones vanilla, o el id
 * @param icon ícono de la estación
 */
public record RecipeStation(String id, String name, ItemStack icon) {

    public static final String CRAFTING = "crafting_table";
    public static final String FURNACE = "furnace";
    public static final String BLASTING = "blast_furnace";
    public static final String SMOKING = "smoker";
    public static final String CAMPFIRE = "campfire";
    public static final String STONECUTTING = "stonecutter";
    public static final String SMITHING = "smithing_table";
    public static final String BREWING = "brewing_stand";
    public static final String ANVIL = "anvil";
    public static final String GRINDSTONE = "grindstone";
    public static final String CARTOGRAPHY = "cartography_table";
    public static final String LOOM = "loom";
    public static final String VILLAGER = "villager";

    public RecipeStation {
        Objects.requireNonNull(id, "id");
        icon = icon == null ? new ItemStack(Material.CRAFTING_TABLE) : icon.clone();
    }

    /** Una estación vanilla: el visor le pone el nombre traducido. */
    public static RecipeStation vanilla(String id, Material icon) {
        return new RecipeStation(id, null, new ItemStack(icon));
    }

    @Override
    public ItemStack icon() {
        return icon.clone();
    }
}

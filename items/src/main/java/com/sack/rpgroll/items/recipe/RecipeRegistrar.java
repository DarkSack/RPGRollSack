package com.sack.rpgroll.items.recipe;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.core.ItemRecipeDef;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Registra en el sistema de crafteo nativo de Bukkit las recetas SHAPED,
 * SHAPELESS, FURNACE y STONECUTTER de cada {@link ItemDefinition}. SMITHING
 * se intenta con la API moderna de smithing table (plantilla+base+adición)
 * y se ignora con un warning si la versión del servidor no la soporta —
 * NPC/PROFESSION/QUEST no se registran acá (ver {@link CustomRecipeRegistry}).
 */
public class RecipeRegistrar {

    private final Plugin plugin;
    private final ItemFactory itemFactory;

    public RecipeRegistrar(Plugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public void registerAll(ItemManager itemManager) {

        int registered = 0;

        for (ItemDefinition definition : itemManager.getAll()) {

            int index = 0;
            for (ItemRecipeDef recipe : definition.recipes()) {
                if (registerOne(definition, recipe, index++)) {
                    registered++;
                }
            }
        }

        plugin.getLogger().info("✔ Recetas registradas: " + registered);
    }

    private boolean registerOne(ItemDefinition definition, ItemRecipeDef recipe, int index) {

        NamespacedKey key = new NamespacedKey(plugin, definition.id() + "-" + index);
        ItemStack result = itemFactory.create(definition);

        try {
            return switch (recipe.type()) {
                case SHAPED -> registerShaped(key, result, recipe);
                case SHAPELESS -> registerShapeless(key, result, recipe);
                case FURNACE -> registerFurnace(key, result, recipe);
                case STONECUTTER -> registerStonecutter(key, result, recipe);
                case SMITHING -> registerSmithing(key, result, recipe);
                case NPC, PROFESSION, QUEST -> false; // datos únicamente — ver CustomRecipeRegistry
            };
        } catch (Exception e) {
            plugin.getLogger().warning("✘ No se pudo registrar receta '" + recipe.type() + "' de '"
                    + definition.id() + "': " + e.getMessage());
            return false;
        }
    }

    private boolean registerShaped(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.shape().isEmpty() || recipe.key().isEmpty()) {
            return false;
        }

        ShapedRecipe shapedRecipe = new ShapedRecipe(key, result);
        shapedRecipe.shape(recipe.shape().toArray(new String[0]));

        for (var entry : recipe.key().entrySet()) {

            Material material = parseMaterial(entry.getValue());
            if (material == null || entry.getKey().isEmpty()) {
                return false;
            }

            shapedRecipe.setIngredient(entry.getKey().charAt(0), material);
        }

        Bukkit.addRecipe(shapedRecipe);
        return true;
    }

    private boolean registerShapeless(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.ingredients().isEmpty()) {
            return false;
        }

        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, result);

        for (String ingredient : recipe.ingredients()) {

            Material material = parseMaterial(ingredient);
            if (material == null) {
                return false;
            }

            shapelessRecipe.addIngredient(material);
        }

        Bukkit.addRecipe(shapelessRecipe);
        return true;
    }

    private boolean registerFurnace(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.ingredients().isEmpty()) {
            return false;
        }

        Material input = parseMaterial(recipe.ingredients().get(0));
        if (input == null) {
            return false;
        }

        FurnaceRecipe furnaceRecipe = new FurnaceRecipe(key, result, input, 0.1f, recipe.cookingTimeTicks());
        Bukkit.addRecipe(furnaceRecipe);
        return true;
    }

    private boolean registerStonecutter(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.ingredients().isEmpty()) {
            return false;
        }

        Material input = parseMaterial(recipe.ingredients().get(0));
        if (input == null) {
            return false;
        }

        StonecuttingRecipe stonecutterRecipe = new StonecuttingRecipe(key, result, new RecipeChoice.MaterialChoice(input));
        Bukkit.addRecipe(stonecutterRecipe);
        return true;
    }

    private boolean registerSmithing(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.baseMaterial() == null || recipe.ingredients().isEmpty()) {
            return false;
        }

        Material base = parseMaterial(recipe.baseMaterial());
        Material addition = parseMaterial(recipe.ingredients().get(0));

        if (base == null || addition == null) {
            return false;
        }

        var smithingRecipe = new org.bukkit.inventory.SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(base),
                new RecipeChoice.MaterialChoice(addition));

        Bukkit.addRecipe(smithingRecipe);
        return true;
    }

    private Material parseMaterial(String raw) {

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

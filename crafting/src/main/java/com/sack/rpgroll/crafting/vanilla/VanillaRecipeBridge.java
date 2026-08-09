package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Registra cada {@link VanillaRecipeDefinition} en el sistema de crafteo
 * nativo de Bukkit ({@code Bukkit.addRecipe}). Cubre mesa de crafteo
 * (shaped/shapeless), la familia completa de hornos, cortadora de piedra y
 * mesa de herrería — las 7 estaciones vanilla para las que Bukkit expone una
 * API de receta genérica. Yunque y fermentación no tienen un tipo Recipe
 * equivalente y se resuelven aparte (ver {@code AnvilEngine}/{@code BrewingEngine}).
 */
public class VanillaRecipeBridge {

    private final Plugin plugin;
    private final RecipeResultFactory resultFactory;

    public VanillaRecipeBridge(Plugin plugin, RecipeResultFactory resultFactory) {
        this.plugin = plugin;
        this.resultFactory = resultFactory;
    }

    public void registerAll(VanillaRecipeManager manager) {

        int registered = 0;

        for (VanillaRecipeDefinition definition : manager.getAll()) {
            if (registerOne(definition)) {
                registered++;
            }
        }

        plugin.getLogger().info("✔ Recetas vanilla registradas: " + registered);
    }

    private boolean registerOne(VanillaRecipeDefinition recipe) {

        ItemStack result = resultFactory.build(recipe.result(), null).orElse(null);
        if (result == null) {
            plugin.getLogger().warning("✘ No se pudo construir el resultado de la receta vanilla '" + recipe.id() + "'");
            return false;
        }

        NamespacedKey key = new NamespacedKey(plugin, recipe.id());

        try {
            return switch (recipe.type()) {
                case CRAFTING_TABLE_SHAPED -> registerShaped(key, result, recipe);
                case CRAFTING_TABLE_SHAPELESS -> registerShapeless(key, result, recipe);
                case FURNACE -> registerFurnace(key, result, recipe);
                case BLAST_FURNACE -> registerBlasting(key, result, recipe);
                case SMOKER -> registerSmoking(key, result, recipe);
                case CAMPFIRE -> registerCampfire(key, result, recipe);
                case STONECUTTER -> registerStonecutter(key, result, recipe);
                case SMITHING_TRANSFORM -> registerSmithing(key, result, recipe);
            };
        } catch (Exception e) {
            plugin.getLogger().warning("✘ No se pudo registrar receta vanilla '" + recipe.id() + "': " + e.getMessage());
            return false;
        }
    }

    private boolean registerShaped(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        if (recipe.shape().isEmpty() || recipe.key().isEmpty()) {
            return false;
        }

        ShapedRecipe shaped = new ShapedRecipe(key, result);
        shaped.shape(recipe.shape().toArray(new String[0]));

        for (var entry : recipe.key().entrySet()) {
            Material material = parseMaterial(entry.getValue());
            if (material == null || entry.getKey().isEmpty()) {
                return false;
            }
            shaped.setIngredient(entry.getKey().charAt(0), material);
        }

        Bukkit.addRecipe(shaped);
        return true;
    }

    private boolean registerShapeless(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        if (recipe.ingredients().isEmpty()) {
            return false;
        }

        ShapelessRecipe shapeless = new ShapelessRecipe(key, result);

        for (String ingredient : recipe.ingredients()) {
            Material material = parseMaterial(ingredient);
            if (material == null) {
                return false;
            }
            shapeless.addIngredient(material);
        }

        Bukkit.addRecipe(shapeless);
        return true;
    }

    private boolean registerFurnace(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material input = firstIngredient(recipe);
        if (input == null) {
            return false;
        }

        Bukkit.addRecipe(new FurnaceRecipe(key, result, input, recipe.experience(), recipe.cookingTimeTicks()));
        return true;
    }

    private boolean registerBlasting(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material input = firstIngredient(recipe);
        if (input == null) {
            return false;
        }

        Bukkit.addRecipe(new BlastingRecipe(key, result, input, recipe.experience(), recipe.cookingTimeTicks()));
        return true;
    }

    private boolean registerSmoking(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material input = firstIngredient(recipe);
        if (input == null) {
            return false;
        }

        Bukkit.addRecipe(new SmokingRecipe(key, result, input, recipe.experience(), recipe.cookingTimeTicks()));
        return true;
    }

    private boolean registerCampfire(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material input = firstIngredient(recipe);
        if (input == null) {
            return false;
        }

        Bukkit.addRecipe(new CampfireRecipe(key, result, input, recipe.experience(), recipe.cookingTimeTicks()));
        return true;
    }

    private boolean registerStonecutter(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material input = firstIngredient(recipe);
        if (input == null) {
            return false;
        }

        Bukkit.addRecipe(new StonecuttingRecipe(key, result, new RecipeChoice.MaterialChoice(input)));
        return true;
    }

    private boolean registerSmithing(NamespacedKey key, ItemStack result, VanillaRecipeDefinition recipe) {

        Material base = parseMaterial(recipe.baseMaterial());
        Material addition = firstIngredient(recipe);

        if (base == null || addition == null) {
            return false;
        }

        Material template = recipe.templateMaterial() != null
                ? parseMaterial(recipe.templateMaterial())
                : Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;

        if (template == null) {
            template = Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
        }

        Bukkit.addRecipe(new SmithingTransformRecipe(key, result,
                new RecipeChoice.MaterialChoice(template),
                new RecipeChoice.MaterialChoice(base),
                new RecipeChoice.MaterialChoice(addition)));
        return true;
    }

    private Material firstIngredient(VanillaRecipeDefinition recipe) {
        return recipe.ingredients().isEmpty() ? null : parseMaterial(recipe.ingredients().get(0));
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

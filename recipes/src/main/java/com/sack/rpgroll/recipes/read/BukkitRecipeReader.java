package com.sack.rpgroll.recipes.read;

import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.common.recipe.RecipeStation;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmithingTrimRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.inventory.TransmuteRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Lee el registro de recetas del servidor ({@code Bukkit.recipeIterator()}): las vanilla y las de
 * CUALQUIER plugin que registre las suyas con {@code Bukkit.addRecipe}, sin importar si las lee
 * de un YAML o las crea en código. Es la fuente que más recetas aporta.
 */
public final class BukkitRecipeReader {

    public static final RecipeStation CRAFTING = RecipeStation.vanilla(RecipeStation.CRAFTING, Material.CRAFTING_TABLE);
    public static final RecipeStation FURNACE = RecipeStation.vanilla(RecipeStation.FURNACE, Material.FURNACE);
    public static final RecipeStation BLASTING = RecipeStation.vanilla(RecipeStation.BLASTING, Material.BLAST_FURNACE);
    public static final RecipeStation SMOKING = RecipeStation.vanilla(RecipeStation.SMOKING, Material.SMOKER);
    public static final RecipeStation CAMPFIRE = RecipeStation.vanilla(RecipeStation.CAMPFIRE, Material.CAMPFIRE);
    public static final RecipeStation STONECUTTING = RecipeStation.vanilla(RecipeStation.STONECUTTING, Material.STONECUTTER);
    public static final RecipeStation SMITHING = RecipeStation.vanilla(RecipeStation.SMITHING, Material.SMITHING_TABLE);

    /** Lo que se leyó y lo que se saltó (recetas "especiales" sin ingredientes fijos, errores). */
    public record Result(List<RecipeEntry> entries, int special, int failed) {
    }

    private final Logger logger;
    private final Notes notes;

    public BukkitRecipeReader(Logger logger, Notes notes) {
        this.logger = logger;
        this.notes = notes;
    }

    public Result read() {

        Map<String, String> sourceByNamespace = new HashMap<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            sourceByNamespace.put(plugin.getName().toLowerCase(Locale.ROOT), plugin.getName());
        }
        sourceByNamespace.put(NamespacedKey.MINECRAFT, "Minecraft");

        List<RecipeEntry> entries = new ArrayList<>();
        int special = 0;
        int failed = 0;
        Iterator<Recipe> iterator = Bukkit.recipeIterator();

        while (iterator.hasNext()) {
            Recipe recipe;
            try {
                recipe = iterator.next();
            } catch (RuntimeException e) {
                failed++;
                continue;
            }
            if (!(recipe instanceof Keyed keyed)) {
                continue;
            }
            NamespacedKey key = keyed.getKey();
            String source = sourceByNamespace.getOrDefault(key.getNamespace(), key.getNamespace());

            try {
                RecipeEntry entry = convert(recipe, key, source);
                if (entry == null) {
                    special++;
                } else {
                    entries.add(entry);
                }
            } catch (RuntimeException e) {
                failed++;
                logger.fine("Receta " + key + " no se pudo leer: " + e);
            }
        }

        return new Result(entries, special, failed);
    }

    private RecipeEntry convert(Recipe recipe, NamespacedKey key, String source) {

        String id = key.asString();

        return switch (recipe) {
            case ShapedRecipe shaped -> shaped(shaped, id, source);
            case ShapelessRecipe shapeless -> {
                RecipeEntry.Builder builder = RecipeEntry.builder(id, CRAFTING).source(source);
                for (RecipeChoice choice : shapeless.getChoiceList()) {
                    builder.input(Choices.slot(choice));
                }
                yield builder.output(shapeless.getResult()).build();
            }
            case TransmuteRecipe transmute -> RecipeEntry.builder(id, CRAFTING).source(source)
                    .input(Choices.slot(transmute.getInput()))
                    .input(Choices.slot(transmute.getMaterial()))
                    .output(transmute.getResult())
                    .note(notes.keepsComponents())
                    .build();
            case CookingRecipe<?> cooking -> RecipeEntry.builder(id, cookingStation(cooking)).source(source)
                    .input(Choices.slot(cooking.getInputChoice()))
                    .output(cooking.getResult())
                    .note(notes.cookingTime(cooking.getCookingTime()))
                    .note(cooking.getExperience() > 0 ? notes.experience(cooking.getExperience()) : null)
                    .build();
            case StonecuttingRecipe cutting -> RecipeEntry.builder(id, STONECUTTING).source(source)
                    .input(Choices.slot(cutting.getInputChoice()))
                    .output(cutting.getResult())
                    .build();
            case SmithingTransformRecipe transform -> RecipeEntry.builder(id, SMITHING).source(source)
                    .input(Choices.slot(transform.getTemplate()))
                    .input(Choices.slot(transform.getBase()))
                    .input(Choices.slot(transform.getAddition()))
                    .output(transform.getResult())
                    .build();
            case SmithingTrimRecipe trim -> {
                RecipeSlot base = Choices.slot(trim.getBase());
                yield RecipeEntry.builder(id, SMITHING).source(source)
                        .input(Choices.slot(trim.getTemplate()))
                        .input(base)
                        .input(Choices.slot(trim.getAddition()))
                        .output(base.isEmpty() ? null : base.options().getFirst())
                        .note(notes.appliesTrim())
                        .build();
            }
            default -> null;
        };
    }

    private static RecipeEntry shaped(ShapedRecipe shaped, String id, String source) {

        String[] rows = shaped.getShape();
        Map<Character, RecipeChoice> choices = shaped.getChoiceMap();
        int width = 0;
        for (String row : rows) {
            width = Math.max(width, row.length());
        }
        width = Math.max(1, Math.min(3, width));

        List<RecipeSlot> slots = new ArrayList<>(9);
        for (int r = 0; r < Math.min(3, rows.length); r++) {
            for (int c = 0; c < width; c++) {
                String row = rows[r];
                RecipeChoice choice = c < row.length() ? choices.get(row.charAt(c)) : null;
                slots.add(Choices.slot(choice));
            }
        }

        return RecipeEntry.builder(id, CRAFTING).source(source).shaped(width, slots).output(shaped.getResult()).build();
    }

    private static RecipeStation cookingStation(CookingRecipe<?> cooking) {
        return switch (cooking) {
            case BlastingRecipe ignored -> BLASTING;
            case SmokingRecipe ignored -> SMOKING;
            case CampfireRecipe ignored -> CAMPFIRE;
            default -> FURNACE;
        };
    }
}

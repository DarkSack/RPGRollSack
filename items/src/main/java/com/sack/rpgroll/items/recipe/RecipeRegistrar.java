package com.sack.rpgroll.items.recipe;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemFactory;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.core.ItemRecipeDef;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Keyed;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registra en el sistema de crafteo nativo de Bukkit las recetas SHAPED,
 * SHAPELESS, FURNACE y STONECUTTER de cada {@link ItemDefinition}. SMITHING
 * se intenta con la API moderna de smithing table (plantilla+base+adición)
 * y se ignora con un warning si la versión del servidor no la soporta —
 * NPC/PROFESSION/QUEST no se registran acá (ver {@link CustomRecipeRegistry}).
 *
 * <p>Un ingrediente {@code item:<id>} es otro ítem de RPGRoll-Items. Bukkit
 * solo compara materiales, así que la receta se registra con el material del
 * ítem y {@link RecipeGuardListener} comprueba al preparar que sea ese ítem y
 * no el material pelado ({@link #constraint}).
 */
public class RecipeRegistrar {

    /**
     * Lo que Bukkit no comprueba de una receta: cuántos de cada ítem de
     * RPGRoll pide y qué materiales van "pelados" (sin ser ítem de RPGRoll).
     */
    public record Constraint(Map<String, Integer> items, Set<Material> plainMaterials) {
    }

    /** Un ingrediente ya resuelto: el material, y el id si es un ítem de RPGRoll. */
    record Ingredient(Material material, String itemId) {
    }

    /** Compartido entre instancias: /itemadmin reload crea un registrador nuevo. */
    private static final Map<NamespacedKey, Constraint> CONSTRAINTS = new ConcurrentHashMap<>();

    private final Plugin plugin;
    private final ItemFactory itemFactory;
    private ItemManager itemManager;

    public RecipeRegistrar(Plugin plugin, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.itemFactory = itemFactory;
    }

    public static Optional<Constraint> constraint(NamespacedKey key) {
        return Optional.ofNullable(CONSTRAINTS.get(key));
    }

    /**
     * Registra las recetas de todos los ítems, quitando antes las que este
     * plugin hubiera registrado. Así sirve igual al arrancar que en
     * {@code /itemadmin reload}: antes solo se llamaba al arrancar, y recargar
     * dejaba sin receta a los ítems nuevos y con receta a los borrados.
     */
    public void registerAll(ItemManager itemManager) {

        this.itemManager = itemManager;
        int removed = unregisterOwn();
        CONSTRAINTS.clear();
        int registered = 0;

        for (ItemDefinition definition : itemManager.getAll()) {

            int index = 0;
            for (ItemRecipeDef recipe : definition.recipes()) {
                if (registerOne(definition, recipe, index++)) {
                    registered++;
                }
            }
        }

        if (removed > 0) {
            // Que los clientes conectados vean el libro de recetas nuevo.
            Bukkit.updateRecipes();
        }

        plugin.getLogger().info("✔ Recetas registradas: " + registered);
    }

    private int unregisterOwn() {

        String namespace = plugin.getName().toLowerCase(Locale.ROOT);
        List<NamespacedKey> own = new ArrayList<>();

        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext(); ) {
            if (it.next() instanceof Keyed keyed && keyed.getKey().getNamespace().equals(namespace)) {
                own.add(keyed.getKey());
            }
        }

        for (NamespacedKey key : own) {
            Bukkit.removeRecipe(key, false);
        }

        return own.size();
    }

    private boolean registerOne(ItemDefinition definition, ItemRecipeDef recipe, int index) {

        NamespacedKey key = new NamespacedKey(plugin, definition.id() + "-" + index);
        ItemStack result = itemFactory.create(definition);
        result.setAmount(Math.min(recipe.amount(), result.getMaxStackSize()));

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

        Map<Character, Ingredient> resolved = new HashMap<>();

        for (var entry : recipe.key().entrySet()) {

            Ingredient ingredient = resolve(entry.getValue());
            if (ingredient == null || entry.getKey().isEmpty()) {
                return false;
            }

            resolved.put(entry.getKey().charAt(0), ingredient);
            shapedRecipe.setIngredient(entry.getKey().charAt(0), ingredient.material());
        }

        List<Ingredient> used = new ArrayList<>();
        for (String row : recipe.shape()) {
            for (char symbol : row.toCharArray()) {
                if (resolved.containsKey(symbol)) {
                    used.add(resolved.get(symbol));
                }
            }
        }

        Bukkit.addRecipe(shapedRecipe);
        remember(key, used);
        return true;
    }

    private boolean registerShapeless(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.ingredients().isEmpty()) {
            return false;
        }

        ShapelessRecipe shapelessRecipe = new ShapelessRecipe(key, result);
        List<Ingredient> used = new ArrayList<>();

        for (String raw : recipe.ingredients()) {

            Ingredient ingredient = resolve(raw);
            if (ingredient == null) {
                return false;
            }

            used.add(ingredient);
            shapelessRecipe.addIngredient(ingredient.material());
        }

        Bukkit.addRecipe(shapelessRecipe);
        remember(key, used);
        return true;
    }

    private boolean registerFurnace(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.singleInput() == null) {
            return false;
        }

        Ingredient input = resolve(recipe.singleInput());
        if (input == null) {
            return false;
        }

        FurnaceRecipe furnaceRecipe = new FurnaceRecipe(key, result, input.material(), 0.1f,
                recipe.cookingTimeTicks());
        Bukkit.addRecipe(furnaceRecipe);
        remember(key, List.of(input));
        return true;
    }

    private boolean registerStonecutter(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.singleInput() == null) {
            return false;
        }

        Ingredient input = resolve(recipe.singleInput());
        if (input == null) {
            return false;
        }

        StonecuttingRecipe stonecutterRecipe = new StonecuttingRecipe(key, result,
                new RecipeChoice.MaterialChoice(input.material()));
        Bukkit.addRecipe(stonecutterRecipe);
        remember(key, List.of(input));
        return true;
    }

    private boolean registerSmithing(NamespacedKey key, ItemStack result, ItemRecipeDef recipe) {

        if (recipe.baseMaterial() == null || recipe.ingredients().isEmpty()) {
            return false;
        }

        Ingredient base = resolve(recipe.baseMaterial());
        Ingredient addition = resolve(recipe.ingredients().get(0));

        if (base == null || addition == null) {
            return false;
        }

        var smithingRecipe = new org.bukkit.inventory.SmithingTransformRecipe(
                key,
                result,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                new RecipeChoice.MaterialChoice(base.material()),
                new RecipeChoice.MaterialChoice(addition.material()));

        Bukkit.addRecipe(smithingRecipe);
        remember(key, List.of(base, addition, new Ingredient(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, null)));
        return true;
    }

    /** {@code MATERIAL} o {@code item:<id>}; null si no existe. */
    private Ingredient resolve(String raw) {

        if (raw != null && raw.trim().regionMatches(true, 0, "item:", 0, 5)) {
            String id = raw.trim().substring(5).trim();
            return itemManager == null ? null
                    : itemManager.get(id).map(definition -> new Ingredient(definition.material(), definition.id()))
                            .orElse(null);
        }

        Material material = parseMaterial(raw);
        return material == null ? null : new Ingredient(material, null);
    }

    private static void remember(NamespacedKey key, List<Ingredient> used) {

        Map<String, Integer> items = new HashMap<>();
        Set<Material> plain = EnumSet.noneOf(Material.class);

        for (Ingredient ingredient : used) {
            if (ingredient.itemId() != null) {
                items.merge(ingredient.itemId(), 1, Integer::sum);
            } else {
                plain.add(ingredient.material());
            }
        }

        CONSTRAINTS.put(key, new Constraint(Map.copyOf(items), plain));
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

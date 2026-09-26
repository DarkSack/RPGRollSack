package com.sack.rpgroll.extras.backpack;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Registra una receta con forma por nivel. Bukkit solo compara materiales,
 * así que lo fino se revisa al preparar el crafteo ({@link #result}): que la
 * cabeza del centro sea de verdad la mochila del nivel anterior y que un
 * {@code item:<id>} sea ese ítem de RPGRoll-Items y no el material pelado.
 */
public class BackpackRecipes {

    private static final String PREFIX = "backpack_";

    private final Plugin plugin;
    private final BackpackService service;
    private final List<NamespacedKey> registered = new ArrayList<>();
    /** Material real de cada ingrediente item:<id>, resuelto al registrar. */
    private final Map<String, Material> itemMaterials = new HashMap<>();

    public BackpackRecipes(Plugin plugin, BackpackService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void register() {

        unregister();

        BackpackSettings settings = service.settings();

        if (!settings.enabled()) {
            return;
        }

        for (BackpackTier tier : settings.tiers()) {

            BackpackRecipe recipe = tier.recipe();

            if (recipe == null) {
                continue;
            }

            NamespacedKey key = new NamespacedKey(plugin, PREFIX + tier.id());
            ShapedRecipe shaped = new ShapedRecipe(key, service.items().create(tier, null, null));
            shaped.shape(recipe.shape().toArray(String[]::new));

            boolean complete = true;

            for (Map.Entry<Character, BackpackIngredient> entry : recipe.ingredients().entrySet()) {

                Optional<Material> material = material(entry.getValue());

                if (material.isEmpty()) {
                    plugin.getLogger().warning("Mochila " + tier.id() + ": el ingrediente item:" + entry.getValue().itemId()
                            + " no existe (¿falta RPGRoll-Items o ese ítem?); el nivel queda sin receta.");
                    complete = false;
                    break;
                }

                shaped.setIngredient(entry.getKey(), new RecipeChoice.MaterialChoice(material.get()));
            }

            if (complete && Bukkit.addRecipe(shaped, true)) {
                registered.add(key);
            }
        }
    }

    public void unregister() {
        registered.forEach(key -> Bukkit.removeRecipe(key, false));
        if (!registered.isEmpty()) {
            Bukkit.updateRecipes();
        }
        registered.clear();
        itemMaterials.clear();
    }

    public void discover(Player player) {
        if (service.settings().discoverRecipes() && !registered.isEmpty()) {
            player.discoverRecipes(registered);
        }
    }

    private Optional<Material> material(BackpackIngredient ingredient) {

        if (ingredient.kind() != BackpackIngredient.Kind.ITEM) {
            return Optional.of(ingredient.material());
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Items")) {
            return Optional.empty();
        }

        Optional<Material> material = ItemsBridge.material(ingredient.itemId());
        material.ifPresent(found -> itemMaterials.put(ingredient.itemId(), found));
        return material;
    }

    // ---------------------------------------------------------------- crafteo

    /** El nivel al que pertenece la receta, si es una de las nuestras. */
    public Optional<BackpackTier> tierOf(Recipe recipe) {

        if (!(recipe instanceof Keyed keyed) || !registered.contains(keyed.getKey())) {
            return Optional.empty();
        }

        return service.settings().tier(keyed.getKey().getKey().substring(PREFIX.length()));
    }

    /**
     * El resultado real de la mesa, o null si los ingredientes no son los que
     * pide la receta. Una mejora conserva el UUID (el contenido) y el dueño.
     */
    public ItemStack result(BackpackTier tier, ItemStack[] matrix) {

        BackpackRecipe recipe = tier.recipe();
        BackpackItems items = service.items();
        UUID carried = null;

        Map<String, Integer> expectedItems = new HashMap<>();
        for (String row : recipe.shape()) {
            for (char symbol : row.toCharArray()) {
                BackpackIngredient ingredient = recipe.ingredients().get(symbol);
                if (ingredient != null && ingredient.kind() == BackpackIngredient.Kind.ITEM) {
                    expectedItems.merge(ingredient.itemId(), 1, Integer::sum);
                }
            }
        }

        Map<String, Integer> foundItems = new HashMap<>();
        String previousTier = service.settings().previous(tier).map(BackpackTier::id).orElse(null);

        for (ItemStack stack : matrix) {

            if (stack == null || stack.getType().isAir()) {
                continue;
            }

            if (stack.getType() == Material.PLAYER_HEAD && recipe.usesBackpack()) {
                // La cabeza tiene que ser la mochila del nivel anterior, no cualquier cabeza.
                if (!items.tierId(stack).map(id -> id.equals(previousTier)).orElse(false)) {
                    return null;
                }
                carried = items.id(stack).orElse(null);
                continue;
            }

            if (items.isBackpack(stack)) {
                return null;
            }

            Optional<String> itemsId = BackpackItems.itemsId(stack);

            if (itemsId.isPresent()) {
                // Un ítem de RPGRoll-Items solo sirve donde la receta lo pide por su id.
                if (!expectedItems.containsKey(itemsId.get())) {
                    return null;
                }
                foundItems.merge(itemsId.get(), 1, Integer::sum);
            } else if (itemMaterials.containsValue(stack.getType()) && !isPlainIngredient(recipe, stack.getType())) {
                // El material pelado de un ítem custom (una amatista cualquiera) no cuenta.
                return null;
            }
        }

        if (!foundItems.equals(expectedItems)) {
            return null;
        }

        return carried == null ? items.create(tier, null, null) : service.itemFor(tier, carried);
    }

    private static boolean isPlainIngredient(BackpackRecipe recipe, Material material) {
        return recipe.ingredients().values().stream()
                .anyMatch(i -> i.kind() == BackpackIngredient.Kind.MATERIAL && i.material() == material);
    }

}

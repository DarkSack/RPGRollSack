package com.sack.rpgroll.items.recipe;

import com.sack.rpgroll.items.instance.ItemInstanceService;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Hace cumplir lo que Bukkit no mira en las recetas:
 * <ul>
 *   <li>un ingrediente {@code item:<id>} tiene que ser ese ítem de RPGRoll, no el material pelado;</li>
 *   <li>un ítem de RPGRoll no se gasta como su material en recetas vanilla ni en las
 *       nuestras que piden el material pelado (un lingote de mitrilo no es hierro);</li>
 *   <li>ni se quema como combustible (un bastón hecho de palo no es leña).</li>
 * </ul>
 * Las recetas de otros plugins no se tocan: cada uno valida las suyas.
 */
public class RecipeGuardListener implements Listener {

    private final String ownNamespace;
    private final NamespacedKey gemKey;
    private final ItemInstanceService instances;
    private final BooleanSupplier protectCustomItems;

    public RecipeGuardListener(Plugin plugin, ItemInstanceService instances, BooleanSupplier protectCustomItems) {
        this.ownNamespace = plugin.getName().toLowerCase(Locale.ROOT);
        this.gemKey = new NamespacedKey(plugin, "gem-id");
        this.instances = instances;
        this.protectCustomItems = protectCustomItems;
    }

    /** true: dejar el resultado; false: los ingredientes no valen para esta receta. */
    boolean allows(Recipe recipe, List<ItemStack> inputs) {

        if (!(recipe instanceof Keyed keyed)) {
            return true;
        }

        NamespacedKey key = keyed.getKey();

        if (key.getNamespace().equals(ownNamespace)) {
            Optional<RecipeRegistrar.Constraint> constraint = RecipeRegistrar.constraint(key);
            return constraint.isEmpty() || matches(constraint.get(), inputs);
        }

        if (key.getNamespace().equals(NamespacedKey.MINECRAFT) && protectCustomItems.getAsBoolean()) {
            return inputs.stream().noneMatch(this::isCustom);
        }

        return true;
    }

    private boolean matches(RecipeRegistrar.Constraint constraint, List<ItemStack> inputs) {

        Map<String, Integer> found = new HashMap<>();

        for (ItemStack item : inputs) {

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            Optional<String> id = instances.getId(item);

            if (id.isEmpty() && isGem(item)) {
                // Una gema de engaste nunca es el material pelado de una receta.
                if (protectCustomItems.getAsBoolean()) {
                    return false;
                }
                continue;
            }

            if (id.isPresent()) {
                if (!constraint.items().containsKey(id.get())) {
                    // Un ítem de RPGRoll donde la receta pide el material pelado.
                    if (protectCustomItems.getAsBoolean()) {
                        return false;
                    }
                    continue;
                }
                found.merge(id.get(), 1, Integer::sum);
            } else if (!constraint.plainMaterials().contains(item.getType())) {
                // El material pelado donde la receta pide un ítem de RPGRoll.
                return false;
            }
        }

        return found.equals(constraint.items());
    }

    private boolean isGem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(gemKey, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private boolean isCustom(ItemStack item) {
        return instances.getId(item).isPresent() || isGem(item);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && !allows(event.getRecipe(), List.of(event.getInventory().getMatrix()))) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {

        Recipe recipe = event.getInventory().getRecipe();
        var inventory = event.getInventory();
        List<ItemStack> inputs = new java.util.ArrayList<>();
        inputs.add(inventory.getInputTemplate());
        inputs.add(inventory.getInputEquipment());
        inputs.add(inventory.getInputMineral());

        if (recipe != null && !allows(recipe, inputs)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(FurnaceBurnEvent event) {
        if (protectCustomItems.getAsBoolean() && isCustom(event.getFuel())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCook(BlockCookEvent event) {
        if (event.getRecipe() != null && !allows(event.getRecipe(), List.of(event.getSource()))) {
            event.setCancelled(true);
        }
    }

}

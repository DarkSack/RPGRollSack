package com.sack.rpgroll.enchantments.effect;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Autofundición. El efecto solo marca el bloque al romperse; lo que suelta
 * se cambia después, en {@link BlockDropItemEvent}, que es cuando el juego ya
 * calculó los drops (con Fortuna incluida). Así sirve igual para el bloque
 * golpeado que para los que rompen el minero de vetas o el martillo.
 * <p>
 * Las recetas son las de horno registradas en el servidor, también las que
 * añade RPGRoll-Items. Un ítem propio (con datos de plugin, modelo o
 * custom model data) solo se funde con una receta exacta para él: si no, un
 * mineral en bruto propio hecho sobre hierro en bruto saldría como lingote
 * de hierro.
 */
public class AutoSmelt implements Listener {

    private final Map<Block, Integer> marked = new HashMap<>();
    private List<FurnaceRecipe> recipes;

    public void mark(EffectContext context) {

        if (!(context.event() instanceof BlockBreakEvent event) || !context.inMainHand()) {
            return;
        }

        if (marked.size() > 512) {
            int now = Bukkit.getCurrentTick();
            marked.values().removeIf(tick -> now - tick > 1);
        }

        marked.put(event.getBlock(), Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {

        Integer tick = marked.remove(event.getBlock());

        if (tick == null || Bukkit.getCurrentTick() - tick > 1) {
            return;
        }

        float experience = 0;

        for (Item drop : event.getItems()) {

            ItemStack stack = drop.getItemStack();
            FurnaceRecipe recipe = recipeFor(stack);

            if (recipe == null) {
                continue;
            }

            ItemStack result = recipe.getResult().clone();
            result.setAmount(Math.min(result.getMaxStackSize(), result.getAmount() * stack.getAmount()));
            drop.setItemStack(result);
            experience += recipe.getExperience() * stack.getAmount();
        }

        int orbs = (int) experience;
        if (ThreadLocalRandom.current().nextFloat() < experience - orbs) {
            orbs++;
        }

        if (orbs > 0) {
            int amount = orbs;
            var location = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
            location.getWorld().spawn(location, ExperienceOrb.class, orb -> orb.setExperience(amount));
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        recipes = null;
    }

    @EventHandler
    public void onResourcesReloaded(ServerResourcesReloadedEvent event) {
        recipes = null;
    }

    private FurnaceRecipe recipeFor(ItemStack stack) {

        if (recipes == null) {
            recipes = new ArrayList<>();
            Bukkit.recipeIterator().forEachRemaining(recipe -> {
                if (recipe instanceof FurnaceRecipe furnace) {
                    recipes.add(furnace);
                }
            });
        }

        boolean custom = isCustom(stack);

        for (FurnaceRecipe recipe : recipes) {
            if (recipe.getInputChoice() instanceof RecipeChoice.ExactChoice exact && exact.test(stack)) {
                return recipe;
            }
        }

        if (custom) {
            return null;
        }

        for (FurnaceRecipe recipe : recipes) {
            if (recipe.getInputChoice() instanceof RecipeChoice.MaterialChoice choice && choice.test(stack)) {
                return recipe;
            }
        }

        return null;
    }

    private static boolean isCustom(ItemStack stack) {

        if (!stack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = stack.getItemMeta();
        return !meta.getPersistentDataContainer().isEmpty() || meta.hasItemModel() || meta.hasCustomModelData();
    }

}

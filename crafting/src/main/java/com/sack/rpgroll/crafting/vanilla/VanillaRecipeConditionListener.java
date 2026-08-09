package com.sack.rpgroll.crafting.vanilla;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Bukkit no permite condicionar una {@code ShapedRecipe}/{@code ShapelessRecipe}
 * en su propia definición, así que esto es lo más cerca que hay: al
 * previsualizarse el resultado en la mesa de crafteo, si la receta es una de
 * las nuestras y tiene {@code conditions}, revalidamos contra el jugador que
 * la está viendo y ocultamos el resultado si no las cumple. Solo aplica a
 * {@code CRAFTING_TABLE_SHAPED}/{@code CRAFTING_TABLE_SHAPELESS} — el resto de
 * estaciones vanilla no tienen un evento de "previsualización" equivalente.
 */
public class VanillaRecipeConditionListener implements Listener {

    private final Plugin plugin;
    private final VanillaRecipeManager recipeManager;
    private final ConditionEvaluator conditionEvaluator;

    public VanillaRecipeConditionListener(Plugin plugin, VanillaRecipeManager recipeManager,
            ConditionEvaluator conditionEvaluator) {
        this.plugin = plugin;
        this.recipeManager = recipeManager;
        this.conditionEvaluator = conditionEvaluator;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {

        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof CraftingRecipe craftingRecipe)) {
            return;
        }

        if (!craftingRecipe.getKey().getNamespace().equalsIgnoreCase(plugin.getName().toLowerCase())) {
            return;
        }

        Optional<VanillaRecipeDefinition> definition = recipeManager.get(craftingRecipe.getKey().getKey());
        if (definition.isEmpty() || definition.get().conditions().isEmpty()) {
            return;
        }

        HumanEntity viewer = event.getView().getPlayer();
        if (!(viewer instanceof Player player)
                || !conditionEvaluator.evaluateAll(definition.get().conditions(), player)) {
            event.getInventory().setResult(null);
        }
    }

}

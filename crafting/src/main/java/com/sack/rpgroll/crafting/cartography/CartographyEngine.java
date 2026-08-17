package com.sack.rpgroll.crafting.cartography;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import io.papermc.paper.event.player.CartographyItemEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sustituye el resultado de la mesa de cartografía cuando el mapa y el ítem
 * puestos coinciden con una {@link CartographyRecipeDefinition}. Bukkit no
 * expone un {@code PrepareCartographyEvent}; Paper sí expone
 * {@link CartographyItemEvent} para reaccionar a cambios en esta mesa —
 * es sobre ese evento que se sobreescribe el resultado vía
 * {@code CartographyInventory#setResult}. Si ninguna receta coincide, no se
 * toca nada y la mesa vanilla sigue funcionando igual.
 * <p>
 * {@code amount} en {@code mapIngredient}/{@code itemIngredient} exige que la
 * pila tenga al menos esa cantidad, pero la mesa vanilla solo consume 1 de
 * cada slot al entregar el resultado — un {@code amount} mayor a 1 no hace
 * que se consuma más que eso. Se recomienda dejarlo en 1 acá.
 */
public class CartographyEngine implements Listener {

    private final CartographyRecipeManager recipeManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;

    public CartographyEngine(CartographyRecipeManager recipeManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory) {
        this.recipeManager = recipeManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
    }

    @EventHandler
    public void onCartographyItem(CartographyItemEvent event) {

        CartographyInventory inventory = event.getInventory();
        ItemStack map = inventory.getItem(0);
        ItemStack item = inventory.getItem(1);

        if (map == null || item == null) {
            return;
        }

        Player player = event.getWhoClicked() instanceof Player p ? p : null;

        for (CartographyRecipeDefinition recipe : recipeManager.getAll()) {

            if (!ingredientMatcher.matchesWithAmount(map, recipe.mapIngredient())
                    || !ingredientMatcher.matchesWithAmount(item, recipe.itemIngredient())) {
                continue;
            }

            if (!recipe.conditions().isEmpty() && (player == null || !conditionEvaluator.evaluateAll(recipe.conditions(), player))) {
                continue;
            }

            resultFactory.build(recipe.result(), null).ifPresent(inventory::setResult);
            return;
        }
    }

}

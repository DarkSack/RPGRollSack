package com.sack.rpgroll.crafting.brewing;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Sustituye el resultado vanilla de una tanda de fermentación cuando el
 * ingrediente coincide con una {@link BrewRecipeDefinition}: cada botella no
 * vacía recibe nuestro resultado personalizado en vez de la poción que
 * Bukkit hubiera calculado. Sin coincidencia, la fermentación vanilla sigue
 * intacta.
 * <p>
 * {@code amount} en {@code ingredient} exige que la pila tenga al menos esa
 * cantidad, pero la fermentación vanilla siempre consume 1 unidad de
 * ingrediente por tanda — un {@code amount} mayor a 1 no hace que se
 * consuma más que eso. Se recomienda dejarlo en 1 acá.
 */
public class BrewingEngine implements Listener {

    private final BrewRecipeManager recipeManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;

    public BrewingEngine(BrewRecipeManager recipeManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory) {
        this.recipeManager = recipeManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
    }

    @EventHandler
    public void onBrew(BrewEvent event) {

        BrewerInventory inventory = event.getContents();
        ItemStack ingredient = inventory.getIngredient();

        if (ingredient == null) {
            return;
        }

        for (BrewRecipeDefinition recipe : recipeManager.getAll()) {

            if (!ingredientMatcher.matchesWithAmount(ingredient, recipe.ingredient())) {
                continue;
            }

            if (!recipe.conditions().isEmpty()) {
                Player viewer = firstViewer(inventory);
                if (viewer == null || !conditionEvaluator.evaluateAll(recipe.conditions(), viewer)) {
                    continue;
                }
            }

            applyResult(event, recipe);
            return;
        }
    }

    private void applyResult(BrewEvent event, BrewRecipeDefinition recipe) {

        resultFactory.build(recipe.result(), null).ifPresent(template -> {

            List<ItemStack> results = event.getResults();

            for (int i = 0; i < results.size(); i++) {
                ItemStack original = results.get(i);
                if (original != null && !original.getType().isAir()) {
                    results.set(i, template.clone());
                }
            }
        });
    }

    private Player firstViewer(BrewerInventory inventory) {

        for (var entity : inventory.getViewers()) {
            if (entity instanceof Player player) {
                return player;
            }
        }

        return null;
    }

}

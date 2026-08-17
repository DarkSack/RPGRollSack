package com.sack.rpgroll.crafting.grindstone;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sustituye el resultado vanilla de la piedra de amolar (quitar
 * encantamientos/combinar) cuando los dos ítems puestos coinciden con una
 * {@link GrindstoneRecipeDefinition}. Si ninguna receta coincide, el evento
 * no se toca y la piedra de amolar vanilla funciona igual que siempre.
 * <p>
 * {@code amount} en {@code upperIngredient}/{@code lowerIngredient} exige que
 * la pila tenga al menos esa cantidad para que la receta aplique, pero la
 * piedra de amolar vanilla solo consume 1 de cada slot al entregar el
 * resultado — un {@code amount} mayor a 1 no hace que se consuma más que eso.
 * Se recomienda dejarlo en 1 acá.
 */
public class GrindstoneEngine implements Listener {

    private final GrindstoneRecipeManager recipeManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;

    public GrindstoneEngine(GrindstoneRecipeManager recipeManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory) {
        this.recipeManager = recipeManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {

        GrindstoneInventory inventory = event.getInventory();
        ItemStack upper = inventory.getUpperItem();
        ItemStack lower = inventory.getLowerItem();

        if (upper == null || lower == null) {
            return;
        }

        for (GrindstoneRecipeDefinition recipe : recipeManager.getAll()) {

            if (!ingredientMatcher.matchesWithAmount(upper, recipe.upperIngredient())
                    || !ingredientMatcher.matchesWithAmount(lower, recipe.lowerIngredient())) {
                continue;
            }

            if (!recipe.conditions().isEmpty()) {
                Player player = event.getView().getPlayer() instanceof Player p ? p : null;
                if (player == null || !conditionEvaluator.evaluateAll(recipe.conditions(), player)) {
                    continue;
                }
            }

            resultFactory.build(recipe.result(), null).ifPresent(event::setResult);
            return;
        }
    }

}

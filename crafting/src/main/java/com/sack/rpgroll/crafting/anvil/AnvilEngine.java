package com.sack.rpgroll.crafting.anvil;

import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Sustituye el resultado vanilla del yunque cuando los dos ítems puestos
 * coinciden con una {@link AnvilRecipeDefinition}: en vez de la reparación o
 * el renombrado normal, el jugador ve nuestro resultado personalizado (con
 * calidad, si la receta la habilita) al costo de niveles configurado.
 * Si ninguna receta coincide, el evento no se toca y el yunque vanilla
 * funciona exactamente igual que siempre.
 * <p>
 * {@code amount} en {@code baseIngredient}/{@code additionIngredient} exige
 * que la pila tenga al menos esa cantidad, pero el yunque vanilla solo
 * consume 1 de cada slot al entregar el resultado — un {@code amount} mayor
 * a 1 no hace que se consuma más que eso. Se recomienda dejarlo en 1 acá.
 */
public class AnvilEngine implements Listener {

    private final AnvilRecipeManager recipeManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;

    public AnvilEngine(AnvilRecipeManager recipeManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory) {
        this.recipeManager = recipeManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {

        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack addition = inventory.getItem(1);

        if (base == null || addition == null) {
            return;
        }

        for (AnvilRecipeDefinition recipe : recipeManager.getAll()) {

            if (!ingredientMatcher.matchesWithAmount(base, recipe.baseIngredient())
                    || !ingredientMatcher.matchesWithAmount(addition, recipe.additionIngredient())) {
                continue;
            }

            if (!recipe.conditions().isEmpty()) {
                Player player = event.getView().getPlayer() instanceof Player p ? p : null;
                if (player == null || !conditionEvaluator.evaluateAll(recipe.conditions(), player)) {
                    continue;
                }
            }

            CraftQuality quality = null; // el yunque no rola calidad — el ítem base ya trae la suya, si tenía
            resultFactory.build(recipe.result(), quality).ifPresent(result -> {
                event.setResult(result);
                inventory.setRepairCostAmount(recipe.repairCostLevels());
            });

            return;
        }
    }

}

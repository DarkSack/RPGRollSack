package com.sack.rpgroll.crafting.discovery;

import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.station.CustomStation;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Resuelve {@code /crafting experiment}: el jugador pone materiales en una
 * estación con {@code allowExperimentation}, sin conocer ninguna receta
 * puntual, y tiene una chance de descubrir la que más se parezca a lo que
 * puso — sin craftearla (solo la marca como descubierta en su Recipe Book,
 * el crafteo real sigue pasando por {@code StationProcessingEngine} como
 * siempre). La chance escala con qué tan cerca estuvo (overlap de ingredientes).
 */
public class ExperimentationService {

    private final CustomRecipeManager recipeManager;
    private final DiscoveryService discoveryService;
    private final IngredientMatcher ingredientMatcher;
    private final Random random = new Random();
    private final Map<UUID, Long> lastAttemptMillis = new HashMap<>();

    private final double baseChance;
    private final double minOverlap;
    private final long cooldownMillis;

    public ExperimentationService(CustomRecipeManager recipeManager, DiscoveryService discoveryService,
            IngredientMatcher ingredientMatcher, double baseChance, double minOverlap, long cooldownSeconds) {
        this.recipeManager = recipeManager;
        this.discoveryService = discoveryService;
        this.ingredientMatcher = ingredientMatcher;
        this.baseChance = Math.min(1, Math.max(0, baseChance));
        this.minOverlap = Math.min(1, Math.max(0, minOverlap));
        this.cooldownMillis = Math.max(0, cooldownSeconds) * 1000L;
    }

    public ExperimentationOutcome attempt(Player player, CustomStation station, Inventory stationInventory) {

        if (!station.allowExperimentation()) {
            return ExperimentationOutcome.of(ExperimentationResult.NOT_ALLOWED);
        }

        long now = System.currentTimeMillis();
        Long last = lastAttemptMillis.get(player.getUniqueId());
        if (last != null && now - last < cooldownMillis) {
            return ExperimentationOutcome.of(ExperimentationResult.ON_COOLDOWN);
        }
        lastAttemptMillis.put(player.getUniqueId(), now);

        PlayerDiscovery discovery = discoveryService.get(player.getUniqueId());
        List<CustomRecipe> undiscovered = recipeManager.byStation(station.id()).stream()
                .filter(recipe -> !discovery.hasDiscovered(recipe.id()))
                .toList();

        if (undiscovered.isEmpty()) {
            return ExperimentationOutcome.of(ExperimentationResult.ALREADY_DISCOVERED_ALL);
        }

        CustomRecipe best = null;
        double bestOverlap = 0;

        for (CustomRecipe recipe : undiscovered) {
            double overlap = overlapRatio(stationInventory, recipe.ingredients());
            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                best = recipe;
            }
        }

        if (best == null || bestOverlap < minOverlap) {
            return ExperimentationOutcome.of(ExperimentationResult.NO_MATCH);
        }

        double chance = baseChance * bestOverlap;
        if (random.nextDouble() > chance) {
            return ExperimentationOutcome.of(ExperimentationResult.NO_MATCH);
        }

        discoveryService.markDiscovered(player.getUniqueId(), best.id());
        return ExperimentationOutcome.discovered(best);
    }

    /** Fracción de ingredientes de la receta que están presentes (aunque sea 1 unidad) en el inventario. */
    private double overlapRatio(Inventory inventory, List<IngredientSpec> ingredients) {

        if (ingredients.isEmpty()) {
            return 0;
        }

        long present = ingredients.stream().filter(spec -> ingredientMatcher.countAvailable(inventory, spec) > 0).count();
        return present / (double) ingredients.size();
    }

}

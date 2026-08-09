package com.sack.rpgroll.crafting.station.runtime;

import com.sack.rpgroll.crafting.api.event.CraftCompleteEvent;
import com.sack.rpgroll.crafting.api.event.CraftFailEvent;
import com.sack.rpgroll.crafting.api.event.CraftPrepareEvent;
import com.sack.rpgroll.crafting.api.event.CraftProcessEvent;
import com.sack.rpgroll.crafting.api.event.CraftStartEvent;
import com.sack.rpgroll.crafting.api.event.RecipeDiscoverEvent;
import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.fuel.FuelDefinition;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.ingredient.IngredientSpec;
import com.sack.rpgroll.crafting.integration.CharacterXpBridge;
import com.sack.rpgroll.crafting.integration.EconomyBridge;
import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.quality.QualityRoller;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;

/**
 * El corazón de una {@code CustomStation}: cada tick de estación (ver
 * {@code station-tick-interval-ticks} en config.yml) recorre todas las
 * estaciones activas, intenta iniciar una receta si hay una libre y sus
 * ingredientes/condiciones/combustible se cumplen, avanza el progreso de la
 * que esté en curso, y entrega el resultado (con calidad, si aplica) al
 * completarse.
 */
public class StationProcessingEngine {

    private final StationRuntimeRegistry registry;
    private final CustomStationManager stationManager;
    private final CustomRecipeManager recipeManager;
    private final FuelManager fuelManager;
    private final IngredientMatcher ingredientMatcher;
    private final ConditionEvaluator conditionEvaluator;
    private final RecipeResultFactory resultFactory;
    private final DiscoveryService discoveryService;
    private final QualityRoller qualityRoller = new QualityRoller();
    private final Random random = new Random();
    private final Logger logger;

    private final double defaultFailChance;
    private final boolean failConsumesIngredients;

    public StationProcessingEngine(StationRuntimeRegistry registry, CustomStationManager stationManager,
            CustomRecipeManager recipeManager, FuelManager fuelManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory,
            DiscoveryService discoveryService, Logger logger,
            double defaultFailChance, boolean failConsumesIngredients) {
        this.registry = registry;
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
        this.discoveryService = discoveryService;
        this.logger = logger;
        this.defaultFailChance = defaultFailChance;
        this.failConsumesIngredients = failConsumesIngredients;
    }

    /** Llamado periódicamente por el scheduler del plugin (ver {@code station-tick-interval-ticks}). */
    public void tick() {

        for (StationRuntime runtime : registry.getAll()) {

            Optional<CustomStation> stationOpt = stationManager.get(runtime.stationDefId());
            if (stationOpt.isEmpty()) {
                continue;
            }

            CustomStation station = stationOpt.get();

            if (station.requiresFuel()) {
                refuelIfNeeded(runtime, station);
            }

            if (!runtime.isProcessing()) {
                tryStart(runtime, station);
                continue;
            }

            if (station.requiresFuel() && runtime.fuelTicksRemaining() <= 0) {
                continue; // sin combustible, no avanza este tick
            }

            runtime.advance();
            if (station.requiresFuel()) {
                runtime.consumeFuelTick();
            }

            Optional<CustomRecipe> recipeOpt = recipeManager.get(runtime.activeRecipeId());
            if (recipeOpt.isEmpty()) {
                runtime.clearRecipe();
                continue;
            }

            CustomRecipe recipe = recipeOpt.get();
            Bukkit.getPluginManager().callEvent(
                    new CraftProcessEvent(recipe, runtime.key(), runtime.progressTicks(), recipe.processingTimeTicks()));

            if (runtime.progressTicks() >= recipe.processingTimeTicks()) {
                complete(runtime, station, recipe);
            }
        }
    }

    private void refuelIfNeeded(StationRuntime runtime, CustomStation station) {

        if (runtime.fuelTicksRemaining() > 0 || station.fuelSlot() < 0) {
            return;
        }

        ItemStack fuelStack = runtime.inventory().getItem(station.fuelSlot());
        Optional<FuelDefinition> fuelDef = fuelManager.matching(fuelStack);

        if (fuelDef.isEmpty() || fuelStack == null) {
            return;
        }

        if (fuelStack.getAmount() < fuelDef.get().consumeAmount()) {
            return;
        }

        fuelStack.setAmount(fuelStack.getAmount() - fuelDef.get().consumeAmount());
        runtime.inventory().setItem(station.fuelSlot(),
                fuelStack.getAmount() <= 0 ? null : fuelStack);
        runtime.addFuelTicks(fuelDef.get().burnTicks());
    }

    private void tryStart(StationRuntime runtime, CustomStation station) {

        List<CustomRecipe> candidates = recipeManager.byStation(station.id());
        Player player = runtime.lastPlayerId() != null ? Bukkit.getPlayer(runtime.lastPlayerId()) : null;

        for (CustomRecipe recipe : candidates) {

            if (!recipe.conditions().isEmpty()) {
                if (player == null || !conditionEvaluator.evaluateAll(recipe.conditions(), player)) {
                    continue;
                }
            }

            if (station.requiresFuel() && recipe.fuelPerCraft() > 0 && runtime.fuelTicksRemaining() < recipe.fuelPerCraft()) {
                continue;
            }

            if (!ingredientsAvailable(runtime.inventory(), recipe.ingredients())) {
                continue;
            }

            CraftPrepareEvent prepareEvent = new CraftPrepareEvent(player, recipe, runtime.key());
            Bukkit.getPluginManager().callEvent(prepareEvent);
            if (prepareEvent.isCancelled()) {
                continue;
            }

            if (recipe.economyCost() > 0) {
                if (player == null || !EconomyBridge.charge(player.getUniqueId(), recipe.economyCurrencyId(),
                        recipe.economyCost(), "Crafting: " + recipe.displayName())) {
                    continue;
                }
            }

            consumeIngredients(runtime.inventory(), recipe.ingredients());
            runtime.startRecipe(recipe.id());
            Bukkit.getPluginManager().callEvent(new CraftStartEvent(player, recipe, runtime.key()));
            return;
        }
    }

    private boolean ingredientsAvailable(Inventory inventory, List<IngredientSpec> ingredients) {

        for (IngredientSpec spec : ingredients) {
            if (ingredientMatcher.countAvailable(inventory, spec) < spec.amount()) {
                return false;
            }
        }
        return true;
    }

    private void consumeIngredients(Inventory inventory, List<IngredientSpec> ingredients) {
        for (IngredientSpec spec : ingredients) {
            ingredientMatcher.tryConsume(inventory, spec);
        }
    }

    private void complete(StationRuntime runtime, CustomStation station, CustomRecipe recipe) {

        double failChance = recipe.failChance() >= 0 ? recipe.failChance() : defaultFailChance;
        boolean failed = failChance > 0 && random.nextDouble() < failChance;

        if (failed) {
            runtime.clearRecipe();
            if (!failConsumesIngredients) {
                // Los ingredientes ya se consumieron al iniciar; sin devolución automática
                // de qué exactamente se consumió, documentar como limitación conocida.
                logger.fine("Receta '" + recipe.id() + "' falló en " + runtime.key());
            }
            Bukkit.getPluginManager().callEvent(new CraftFailEvent(recipe, runtime.key()));
            return;
        }

        double skillFactor = 0.5;
        Player player = runtime.lastPlayerId() != null ? Bukkit.getPlayer(runtime.lastPlayerId()) : null;

        CraftQuality quality = recipe.qualityEnabled() ? qualityRoller.roll(skillFactor) : null;
        Optional<ItemStack> result = resultFactory.build(recipe.result(), quality);

        if (result.isEmpty()) {
            logger.warning("✘ No se pudo construir el resultado de la receta '" + recipe.id() + "'");
            runtime.clearRecipe();
            return;
        }

        if (!placeInOutputSlot(runtime.inventory(), station.outputSlot(), result.get())) {
            return; // slot de salida ocupado — reintenta el próximo tick sin perder el progreso
        }

        runtime.clearRecipe();

        if (recipe.xpAmount() > 0 && player != null) {
            CharacterXpBridge.grant(player.getUniqueId(), recipe.xpAmount());
        }

        if (player != null) {
            Bukkit.getPluginManager().callEvent(new CraftCompleteEvent(player, recipe, result.get(), quality));

            if (discoveryService.markDiscovered(player.getUniqueId(), recipe.id())) {
                player.sendMessage("§b§lNueva receta descubierta: §f" + recipe.displayName());
                Bukkit.getPluginManager().callEvent(new RecipeDiscoverEvent(player, recipe));
            }
        }
    }

    private boolean placeInOutputSlot(Inventory inventory, int outputSlot, ItemStack result) {

        ItemStack current = inventory.getItem(outputSlot);

        if (current == null || current.getType().isAir()) {
            inventory.setItem(outputSlot, result);
            return true;
        }

        if (!current.isSimilar(result)) {
            return false;
        }

        int maxStack = current.getMaxStackSize();
        if (current.getAmount() + result.getAmount() > maxStack) {
            return false;
        }

        current.setAmount(current.getAmount() + result.getAmount());
        return true;
    }

}

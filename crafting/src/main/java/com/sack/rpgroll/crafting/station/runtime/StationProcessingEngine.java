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
import com.sack.rpgroll.crafting.proficiency.ProficiencyService;
import com.sack.rpgroll.crafting.quality.CraftQuality;
import com.sack.rpgroll.crafting.quality.QualityRoller;
import com.sack.rpgroll.crafting.recipe.CustomRecipe;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * El corazón de una {@code CustomStation}: cada tick de estación (ver
 * {@code station-tick-interval-ticks} en config.yml) recorre todas las
 * estaciones activas, intenta iniciar una receta si hay una libre y sus
 * ingredientes/condiciones/combustible se cumplen, avanza el progreso de la
 * que esté en curso, y entrega el resultado (con calidad, si aplica) al
 * completarse.
 * <p>
 * El dueño de la estación ({@code runtime.lastPlayerId()}) NO necesita estar
 * conectado para que esto siga andando: las condiciones que solo dependen de
 * su UUID (nivel/raza/clase/job/guild) se evalúan igual offline vía
 * {@link ConditionEvaluator#evaluateAllOffline}, el cobro de Economy ya
 * funciona por UUID, y el xp de personaje/proficiencia también. Solo las
 * condiciones atadas a su posición real (PERMISSION/BIOME) siguen exigiendo
 * que esté conectado — documentado en {@link ConditionEvaluator}.
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
    private final ProficiencyService proficiencyService;
    private final QualityRoller qualityRoller = new QualityRoller();
    private final Random random = new Random();
    private final Logger logger;

    private final double defaultFailChance;
    private final boolean failConsumesIngredients;
    private final LangManager lang;

    public StationProcessingEngine(StationRuntimeRegistry registry, CustomStationManager stationManager,
            CustomRecipeManager recipeManager, FuelManager fuelManager, IngredientMatcher ingredientMatcher,
            ConditionEvaluator conditionEvaluator, RecipeResultFactory resultFactory,
            DiscoveryService discoveryService, ProficiencyService proficiencyService, Logger logger,
            double defaultFailChance, boolean failConsumesIngredients, LangManager lang) {
        this.registry = registry;
        this.stationManager = stationManager;
        this.recipeManager = recipeManager;
        this.fuelManager = fuelManager;
        this.ingredientMatcher = ingredientMatcher;
        this.conditionEvaluator = conditionEvaluator;
        this.resultFactory = resultFactory;
        this.discoveryService = discoveryService;
        this.proficiencyService = proficiencyService;
        this.logger = logger;
        this.defaultFailChance = defaultFailChance;
        this.failConsumesIngredients = failConsumesIngredients;
        this.lang = lang;
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
            int requiredTicks = effectiveProcessingTicks(station, runtime.tier(), recipe.processingTimeTicks());
            Bukkit.getPluginManager().callEvent(
                    new CraftProcessEvent(recipe, runtime.key(), runtime.progressTicks(), requiredTicks));

            if (runtime.progressTicks() >= requiredTicks) {
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
        UUID playerId = runtime.lastPlayerId();
        Player onlinePlayer = playerId != null ? Bukkit.getPlayer(playerId) : null;
        World stationWorld = Bukkit.getWorld(runtime.world());

        for (CustomRecipe recipe : candidates) {

            if (!recipe.conditions().isEmpty()) {
                if (playerId == null
                        || !conditionEvaluator.evaluateAllOffline(recipe.conditions(), playerId, onlinePlayer, stationWorld)) {
                    continue;
                }
            }

            if (station.requiresFuel() && recipe.fuelPerCraft() > 0 && runtime.fuelTicksRemaining() < recipe.fuelPerCraft()) {
                continue;
            }

            if (!ingredientsAvailable(runtime.inventory(), recipe.ingredients())) {
                continue;
            }

            CraftPrepareEvent prepareEvent = new CraftPrepareEvent(onlinePlayer, recipe, runtime.key());
            Bukkit.getPluginManager().callEvent(prepareEvent);
            if (prepareEvent.isCancelled()) {
                continue;
            }

            if (recipe.economyCost() > 0) {
                if (playerId == null || !EconomyBridge.charge(playerId, recipe.economyCurrencyId(),
                        recipe.economyCost(), "Crafting: " + recipe.displayName())) {
                    continue;
                }
            }

            List<ItemStack> consumed = consumeIngredients(runtime.inventory(), recipe.ingredients());
            runtime.startRecipe(recipe.id());
            runtime.setConsumedForCurrentRecipe(consumed);
            Bukkit.getPluginManager().callEvent(new CraftStartEvent(onlinePlayer, recipe, runtime.key()));
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

    private List<ItemStack> consumeIngredients(Inventory inventory, List<IngredientSpec> ingredients) {
        List<ItemStack> consumed = new ArrayList<>();
        for (IngredientSpec spec : ingredients) {
            consumed.addAll(ingredientMatcher.tryConsumeAndCapture(inventory, spec));
        }
        return consumed;
    }

    /** Nivel de estación reduce el tiempo de proceso; nunca por debajo de 1 tick. */
    private int effectiveProcessingTicks(CustomStation station, int tier, int baseTicks) {
        double reduction = station.speedBonusPerTier() * (tier - 1);
        return Math.max(1, (int) Math.round(baseTicks * (1 - Math.min(1, reduction))));
    }

    /** Nivel de estación reduce la probabilidad de fallo; nunca por debajo de 0. */
    private double effectiveFailChance(CustomStation station, int tier, double baseChance) {
        double reduction = station.failReductionPerTier() * (tier - 1);
        return Math.max(0, baseChance - reduction);
    }

    private void complete(StationRuntime runtime, CustomStation station, CustomRecipe recipe) {

        double baseFailChance = recipe.failChance() >= 0 ? recipe.failChance() : defaultFailChance;
        double failChance = effectiveFailChance(station, runtime.tier(), baseFailChance);
        boolean failed = failChance > 0 && random.nextDouble() < failChance;

        if (failed) {
            List<ItemStack> consumed = runtime.consumedForCurrentRecipe();
            runtime.clearRecipe();
            if (!failConsumesIngredients) {
                returnIngredients(runtime, consumed);
            }
            Bukkit.getPluginManager().callEvent(new CraftFailEvent(recipe, runtime.key()));
            return;
        }

        UUID playerId = runtime.lastPlayerId();
        double skillFactor = proficiencyService.factor(playerId, station.skillCategory());

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

        if (playerId != null) {

            if (recipe.xpAmount() > 0) {
                CharacterXpBridge.grant(playerId, recipe.xpAmount());
            }
            proficiencyService.grantXp(playerId, station.skillCategory(), Math.max(1, recipe.xpAmount()));

            boolean newlyDiscovered = discoveryService.markDiscovered(playerId, recipe.id());

            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer != null) {
                Bukkit.getPluginManager().callEvent(new CraftCompleteEvent(onlinePlayer, recipe, result.get(), quality));

                if (newlyDiscovered) {
                    lang.send(onlinePlayer, "station.recipe_discovered", "recipe", recipe.displayName());
                    Bukkit.getPluginManager().callEvent(new RecipeDiscoverEvent(onlinePlayer, recipe));
                }
            }
        }
    }

    /**
     * Devuelve los ingredientes que se habían consumido al iniciar una receta
     * que terminó fallando (solo si {@code fail-consumes-ingredients: false}).
     * Primero intenta acomodarlos en el propio inventario de la estación; lo
     * que no entre se tira al piso en la ubicación del bloque, para no
     * perderlo silenciosamente si la estación está llena.
     */
    private void returnIngredients(StationRuntime runtime, List<ItemStack> consumed) {

        if (consumed.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> leftover = runtime.inventory().addItem(consumed.toArray(new ItemStack[0]));
        if (leftover.isEmpty()) {
            return;
        }

        World world = Bukkit.getWorld(runtime.world());
        if (world == null) {
            logger.warning("✘ No se pudieron devolver ingredientes en " + runtime.key() + ": el mundo no está cargado.");
            return;
        }

        Location dropLocation = new Location(world, runtime.x() + 0.5, runtime.y() + 0.5, runtime.z() + 0.5);
        for (ItemStack item : leftover.values()) {
            world.dropItemNaturally(dropLocation, item);
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

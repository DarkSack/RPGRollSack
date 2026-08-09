package com.sack.rpgroll.crafting;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.crafting.anvil.AnvilEngine;
import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.api.CraftingAPI;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewingEngine;
import com.sack.rpgroll.crafting.command.CraftingAdminCommand;
import com.sack.rpgroll.crafting.command.CraftingCommand;
import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.gui.ChatPromptManager;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.quality.CraftQualityRankResolver;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.listener.StationBlockInteractListener;
import com.sack.rpgroll.crafting.station.listener.StationInventoryGuardListener;
import com.sack.rpgroll.crafting.station.runtime.StationProcessingEngine;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeStore;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeConditionListener;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Punto de entrada de RPGRoll-Crafting: recetas personalizadas con
 * ingredientes/condiciones ricas, sistema de calidad, estaciones de crafteo
 * propias (multi-etapa, con combustible), puente hacia las estaciones
 * vanilla que exponen una API de receta genérica (mesa de crafteo, familia
 * de hornos, cortadora de piedra, mesa de herrería) y motores dedicados de
 * yunque y fermentación para las dos que no la tienen.
 */
public class CraftingPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("recipes", "stations", "fuels", "vanilla-recipes",
            "anvil-recipes", "brew-recipes");

    private CustomStationManager stationManager;
    private CustomRecipeManager recipeManager;
    private FuelManager fuelManager;
    private VanillaRecipeManager vanillaRecipeManager;
    private AnvilRecipeManager anvilRecipeManager;
    private BrewRecipeManager brewRecipeManager;
    private DiscoveryService discoveryService;

    private StationRuntimeRegistry stationRuntimeRegistry;
    private StationRuntimeStore stationRuntimeStore;
    private StationProcessingEngine stationProcessingEngine;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        stationManager = new CustomStationManager(this);
        stationManager.initialize();

        recipeManager = new CustomRecipeManager(this);
        recipeManager.initialize();

        fuelManager = new FuelManager(this);
        fuelManager.initialize();

        vanillaRecipeManager = new VanillaRecipeManager(this);
        vanillaRecipeManager.initialize();

        anvilRecipeManager = new AnvilRecipeManager(this);
        anvilRecipeManager.initialize();

        brewRecipeManager = new BrewRecipeManager(this);
        brewRecipeManager.initialize();

        discoveryService = new DiscoveryService(getDataFolder(), getLogger());

        stationRuntimeStore = new StationRuntimeStore(getDataFolder(), stationManager, getLogger());
        stationRuntimeRegistry = new StationRuntimeRegistry(stationRuntimeStore);
        stationRuntimeRegistry.loadAll();

        IngredientMatcher ingredientMatcher = new IngredientMatcher(new CraftQualityRankResolver());
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        RecipeResultFactory resultFactory = new RecipeResultFactory();

        stationProcessingEngine = new StationProcessingEngine(stationRuntimeRegistry, stationManager, recipeManager,
                fuelManager, ingredientMatcher, conditionEvaluator, resultFactory, discoveryService, getLogger(),
                getConfig().getDouble("default-fail-chance", 0.0),
                getConfig().getBoolean("fail-consumes-ingredients", true));

        CraftingAPI.init(recipeManager, stationManager, fuelManager, vanillaRecipeManager, anvilRecipeManager,
                brewRecipeManager, discoveryService, stationRuntimeRegistry);

        registerVanillaRecipes();

        getServer().getPluginManager().registerEvents(
                new StationBlockInteractListener(stationManager, stationRuntimeRegistry), this);
        getServer().getPluginManager().registerEvents(
                new StationInventoryGuardListener(stationManager, stationRuntimeRegistry), this);
        getServer().getPluginManager().registerEvents(
                new VanillaRecipeConditionListener(this, vanillaRecipeManager, conditionEvaluator), this);
        getServer().getPluginManager().registerEvents(
                new AnvilEngine(anvilRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new BrewingEngine(brewRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        registerCommands(chatPromptManager);
        startTasks();

        getLogger().info("✔ RPGRoll-Crafting habilitado. " + stationManager.count() + " estación(es), "
                + recipeManager.count() + " receta(s) personalizada(s), " + vanillaRecipeManager.count()
                + " receta(s) vanilla, " + fuelManager.count() + " combustible(s).");
    }

    @Override
    public void onDisable() {

        if (stationRuntimeRegistry != null) {
            stationRuntimeRegistry.saveAll();
        }
    }

    private void registerVanillaRecipes() {
        RecipeResultFactory resultFactory = new RecipeResultFactory();
        new VanillaRecipeBridge(this, resultFactory).registerAll(vanillaRecipeManager);
    }

    private void registerCommands(ChatPromptManager chatPromptManager) {

        var adminCommand = getCommand("craftingadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'craftingadmin' no está declarado en plugin.yml");
        } else {
            var executor = new CraftingAdminCommand(stationManager, recipeManager, fuelManager, vanillaRecipeManager,
                    anvilRecipeManager, brewRecipeManager, chatPromptManager, this::reloadContent);
            adminCommand.setExecutor(executor);
            adminCommand.setTabCompleter(executor);
        }

        var playerCommand = getCommand("crafting");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'crafting' no está declarado en plugin.yml");
        } else {
            var executor = new CraftingCommand(recipeManager, stationManager, discoveryService);
            playerCommand.setExecutor(executor);
            playerCommand.setTabCompleter(executor);
        }
    }

    private void reloadContent() {
        stationManager.reload();
        recipeManager.reload();
        fuelManager.reload();
        vanillaRecipeManager.reload();
        anvilRecipeManager.reload();
        brewRecipeManager.reload();
    }

    private void startTasks() {

        long stationTickInterval = getConfig().getLong("station-tick-interval-ticks", 20);
        long autosaveInterval = getConfig().getLong("station-autosave-interval-ticks", 6000);

        getServer().getScheduler().runTaskTimer(this, stationProcessingEngine::tick, stationTickInterval,
                stationTickInterval);
        getServer().getScheduler().runTaskTimer(this, stationRuntimeRegistry::saveAll, autosaveInterval,
                autosaveInterval);
    }

}

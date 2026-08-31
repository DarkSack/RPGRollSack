package com.sack.rpgroll.crafting;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.crafting.anvil.AnvilEngine;
import com.sack.rpgroll.crafting.anvil.AnvilRecipeManager;
import com.sack.rpgroll.crafting.api.CraftingAPI;
import com.sack.rpgroll.crafting.brewing.BrewRecipeManager;
import com.sack.rpgroll.crafting.brewing.BrewingEngine;
import com.sack.rpgroll.crafting.cartography.CartographyEngine;
import com.sack.rpgroll.crafting.cartography.CartographyRecipeManager;
import com.sack.rpgroll.crafting.command.CraftingAdminCommand;
import com.sack.rpgroll.crafting.command.CraftingCommand;
import com.sack.rpgroll.crafting.condition.ConditionEvaluator;
import com.sack.rpgroll.crafting.crafter.CrafterAutomationEngine;
import com.sack.rpgroll.crafting.discovery.DiscoveryService;
import com.sack.rpgroll.crafting.discovery.ExperimentationService;
import com.sack.rpgroll.crafting.fuel.FuelManager;
import com.sack.rpgroll.crafting.grindstone.GrindstoneEngine;
import com.sack.rpgroll.crafting.grindstone.GrindstoneRecipeManager;
import com.sack.rpgroll.crafting.gui.ChatPromptManager;
import com.sack.rpgroll.crafting.ingredient.IngredientMatcher;
import com.sack.rpgroll.crafting.loom.LoomEngine;
import com.sack.rpgroll.crafting.loom.LoomRecipeManager;
import com.sack.rpgroll.crafting.proficiency.ProficiencyService;
import com.sack.rpgroll.crafting.quality.CraftQualityRankResolver;
import com.sack.rpgroll.crafting.recipe.CustomRecipeManager;
import com.sack.rpgroll.crafting.recipe.RecipeResultFactory;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.listener.StationBlockInteractListener;
import com.sack.rpgroll.crafting.station.listener.StationInventoryGuardListener;
import com.sack.rpgroll.crafting.station.runtime.StationProcessingEngine;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeStore;
import com.sack.rpgroll.crafting.station.tier.StationUpgradeService;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeBridge;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeConditionListener;
import com.sack.rpgroll.crafting.vanilla.VanillaRecipeManager;
import com.sack.rpgroll.crafting.villager.VillagerTradeEngine;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Punto de entrada de RPGRoll-Crafting: recetas personalizadas con
 * ingredientes/condiciones ricas, sistema de calidad, estaciones de crafteo
 * propias (multi-etapa, con combustible, estructura multibloque opcional y
 * niveles mejorables), puente hacia las estaciones vanilla que exponen una
 * API de receta genérica (mesa de crafteo, familia de hornos, cortadora de
 * piedra, mesa de herrería) y motores dedicados para las que no la tienen
 * (yunque, fermentación, piedra de amolar, mesa de cartografía, telar,
 * comercio de aldeanos, automatización de Crafter). Incluye además un
 * sistema de proficiencia propio (reemplaza el skillFactor de calidad) y
 * descubrimiento de recetas por experimentación.
 */
public class CraftingPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("recipes", "stations", "fuels", "vanilla-recipes",
            "anvil-recipes", "brew-recipes", "grindstone-recipes", "cartography-recipes", "loom-recipes",
            "villager-trades");

    private CustomStationManager stationManager;
    private CustomRecipeManager recipeManager;
    private FuelManager fuelManager;
    private VanillaRecipeManager vanillaRecipeManager;
    private AnvilRecipeManager anvilRecipeManager;
    private BrewRecipeManager brewRecipeManager;
    private GrindstoneRecipeManager grindstoneRecipeManager;
    private CartographyRecipeManager cartographyRecipeManager;
    private LoomRecipeManager loomRecipeManager;
    private VillagerTradeManager villagerTradeManager;
    private DiscoveryService discoveryService;
    private ProficiencyService proficiencyService;

    private StationRuntimeRegistry stationRuntimeRegistry;
    private StationRuntimeStore stationRuntimeStore;
    private StationProcessingEngine stationProcessingEngine;
    private VanillaRecipeBridge vanillaRecipeBridge;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        // LangManager - Mensajes por idioma (lang/es.yml, en.yml, pt_BR.yml)
        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

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

        grindstoneRecipeManager = new GrindstoneRecipeManager(this);
        grindstoneRecipeManager.initialize();

        cartographyRecipeManager = new CartographyRecipeManager(this);
        cartographyRecipeManager.initialize();

        loomRecipeManager = new LoomRecipeManager(this);
        loomRecipeManager.initialize();

        villagerTradeManager = new VillagerTradeManager(this);
        villagerTradeManager.initialize();

        discoveryService = new DiscoveryService(getDataFolder(), getLogger());
        proficiencyService = new ProficiencyService(getDataFolder(), getLogger());

        stationRuntimeStore = new StationRuntimeStore(getDataFolder(), stationManager, getLogger());
        stationRuntimeRegistry = new StationRuntimeRegistry(stationRuntimeStore);
        stationRuntimeRegistry.loadAll();

        IngredientMatcher ingredientMatcher = new IngredientMatcher(new CraftQualityRankResolver());
        ConditionEvaluator conditionEvaluator = new ConditionEvaluator();
        RecipeResultFactory resultFactory = new RecipeResultFactory();

        StationUpgradeService stationUpgradeService = new StationUpgradeService(stationManager, stationRuntimeRegistry,
                ingredientMatcher);
        ExperimentationService experimentationService = new ExperimentationService(recipeManager, discoveryService,
                ingredientMatcher, getConfig().getDouble("experimentation-base-chance", 0.35),
                getConfig().getDouble("experimentation-min-overlap", 0.4),
                getConfig().getLong("experimentation-cooldown-seconds", 5));

        stationProcessingEngine = new StationProcessingEngine(stationRuntimeRegistry, stationManager, recipeManager,
                fuelManager, ingredientMatcher, conditionEvaluator, resultFactory, discoveryService, proficiencyService,
                getLogger(), getConfig().getDouble("default-fail-chance", 0.0),
                getConfig().getBoolean("fail-consumes-ingredients", true), langManager);

        CraftingAPI.init(recipeManager, stationManager, fuelManager, vanillaRecipeManager, anvilRecipeManager,
                brewRecipeManager, grindstoneRecipeManager, cartographyRecipeManager, loomRecipeManager,
                villagerTradeManager, discoveryService, proficiencyService, stationRuntimeRegistry);

        registerVanillaRecipes();

        getServer().getPluginManager().registerEvents(
                new StationBlockInteractListener(stationManager, stationRuntimeRegistry, langManager), this);
        getServer().getPluginManager().registerEvents(
                new StationInventoryGuardListener(stationManager, stationRuntimeRegistry), this);
        getServer().getPluginManager().registerEvents(
                new VanillaRecipeConditionListener(this, vanillaRecipeManager, conditionEvaluator), this);
        getServer().getPluginManager().registerEvents(
                new AnvilEngine(anvilRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new BrewingEngine(brewRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new GrindstoneEngine(grindstoneRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new CartographyEngine(cartographyRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new LoomEngine(loomRecipeManager, ingredientMatcher, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new VillagerTradeEngine(this, villagerTradeManager, conditionEvaluator, resultFactory), this);
        getServer().getPluginManager().registerEvents(
                new CrafterAutomationEngine(this, vanillaRecipeManager), this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        registerCommands(chatPromptManager, stationUpgradeService, experimentationService);
        startTasks();

        getLogger().info("✔ RPGRoll-Crafting habilitado. " + stationManager.count() + " estación(es), "
                + recipeManager.count() + " receta(s) personalizada(s), " + vanillaRecipeManager.count()
                + " receta(s) vanilla, " + fuelManager.count() + " combustible(s), "
                + (grindstoneRecipeManager.count() + cartographyRecipeManager.count() + loomRecipeManager.count())
                + " receta(s) de amolar/cartografía/telar, " + villagerTradeManager.count() + " comercio(s) de aldeano.");
    }

    @Override
    public void onDisable() {

        if (stationRuntimeRegistry != null) {
            stationRuntimeRegistry.saveAll();
        }
    }

    private void registerVanillaRecipes() {
        vanillaRecipeBridge = new VanillaRecipeBridge(this, new RecipeResultFactory());
        vanillaRecipeBridge.registerAll(vanillaRecipeManager);
    }

    private void registerCommands(ChatPromptManager chatPromptManager, StationUpgradeService stationUpgradeService,
            ExperimentationService experimentationService) {

        var adminCommand = getCommand("craftingadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'craftingadmin' no está declarado en plugin.yml");
        } else {
            var executor = new CraftingAdminCommand(this, stationManager, recipeManager, fuelManager,
                    vanillaRecipeManager, anvilRecipeManager, brewRecipeManager, grindstoneRecipeManager,
                    cartographyRecipeManager, loomRecipeManager, vanillaRecipeBridge, villagerTradeManager,
                    chatPromptManager, this::reloadContent, langManager);
            adminCommand.setExecutor(executor);
            adminCommand.setTabCompleter(executor);
        }

        var playerCommand = getCommand("crafting");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'crafting' no está declarado en plugin.yml");
        } else {
            var executor = new CraftingCommand(recipeManager, stationManager, discoveryService, stationRuntimeRegistry,
                    stationUpgradeService, experimentationService, proficiencyService, langManager);
            playerCommand.setExecutor(executor);
            playerCommand.setTabCompleter(executor);
        }
    }

    private void reloadContent() {
        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));
        stationManager.reload();
        recipeManager.reload();
        fuelManager.reload();
        vanillaRecipeManager.getAll().forEach(recipe -> vanillaRecipeBridge.unregister(recipe.id()));
        vanillaRecipeManager.reload();
        vanillaRecipeBridge.registerAll(vanillaRecipeManager);
        anvilRecipeManager.reload();
        brewRecipeManager.reload();
        grindstoneRecipeManager.reload();
        cartographyRecipeManager.reload();
        loomRecipeManager.reload();
        villagerTradeManager.reload();
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

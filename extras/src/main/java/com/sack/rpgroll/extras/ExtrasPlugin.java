package com.sack.rpgroll.extras;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.extras.backpack.BackpackCommand;
import com.sack.rpgroll.extras.backpack.BackpackConfigParser;
import com.sack.rpgroll.extras.backpack.BackpackItems;
import com.sack.rpgroll.extras.backpack.BackpackListener;
import com.sack.rpgroll.extras.backpack.BackpackRecipes;
import com.sack.rpgroll.extras.backpack.BackpackService;
import com.sack.rpgroll.extras.backpack.BackpackStorage;
import com.sack.rpgroll.extras.menu.ExtrasMenuManager;
import com.sack.rpgroll.extras.menu.MenuCommand;
import com.sack.rpgroll.extras.menu.ServerMenu;
import com.sack.rpgroll.extras.menu.ServerMenuConfig;
import com.sack.rpgroll.extras.menu.ServerMenuListener;
import com.sack.rpgroll.extras.action.ExtrasActionExecutor;
import com.sack.rpgroll.extras.activity.ActivityStateResolver;
import com.sack.rpgroll.extras.activity.AfkPolicy;
import com.sack.rpgroll.extras.activity.CombatTagListener;
import com.sack.rpgroll.extras.api.ExtrasAPI;
import com.sack.rpgroll.extras.api.NeedsService;
import com.sack.rpgroll.extras.api.StatesService;
import com.sack.rpgroll.extras.command.ExtrasAdminCommand;
import com.sack.rpgroll.extras.condition.ConditionManager;
import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.consumption.ConsumptionListener;
import com.sack.rpgroll.extras.expression.RateConditionEvaluator;
import com.sack.rpgroll.extras.hud.HudEngine;
import com.sack.rpgroll.extras.hud.HudSettings;
import com.sack.rpgroll.extras.hud.HudSettingsLoader;
import com.sack.rpgroll.extras.integration.TabIntegration;
import com.sack.rpgroll.extras.listener.PlayerLifecycleListener;
import com.sack.rpgroll.extras.modifier.ModifierManager;
import com.sack.rpgroll.extras.modifier.ModifierResolver;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.extras.temperature.AmbientTemperatureCalculator;
import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;
import com.sack.rpgroll.extras.temperature.TemperatureSettings;
import com.sack.rpgroll.extras.temperature.TemperatureSettingsLoader;
import com.sack.rpgroll.extras.thermal.ThermalProtectionService;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public class ExtrasPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("stats", "conditions", "modifiers", "menus");

    private LangManager langManager;
    private ExtrasMenuManager menuManager;
    private ServerMenu serverMenu;
    private BackpackService backpackService;
    private BackpackRecipes backpackRecipes;
    private StatManager statManager;
    private StatEngine statEngine;
    private ConditionManager conditionManager;
    private ConditionRuntime conditionRuntime;
    private ModifierManager modifierManager;
    private BodyTemperatureEngine temperatureEngine;
    private HudEngine hudEngine;
    private ActivityStateResolver activityStateResolver;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);
        new ResourceCopier(this).copyFiles(List.of(
                new com.sack.rpgroll.common.resource.ResourceFile("temperature.yml", "temperature.yml", false),
                new com.sack.rpgroll.common.resource.ResourceFile("hud.yml", "hud.yml", false),
                new com.sack.rpgroll.common.resource.ResourceFile("backpacks.yml", "backpacks.yml", false)));

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        menuManager = new ExtrasMenuManager(this);
        menuManager.initialize();
        serverMenu = new ServerMenu(this, menuManager, langManager);
        serverMenu.configure(ServerMenuConfig.from(getConfig().getConfigurationSection("server-menu")));
        getServer().getPluginManager().registerEvents(new ServerMenuListener(this, serverMenu), this);

        backpackService = new BackpackService(new BackpackItems(this), new BackpackStorage(getDataFolder(), getLogger()),
                langManager);
        backpackRecipes = new BackpackRecipes(this, backpackService);
        configureBackpacks();
        getServer().getPluginManager().registerEvents(
                new BackpackListener(this, backpackService, backpackRecipes, langManager), this);

        loadAndWire();

        registerCommand();

        getLogger().info("✔ RPGRoll-Extras habilitado. " + statManager.count() + " stat(s), "
                + conditionManager.count() + " condition(s), " + modifierManager.count() + " modificador(es).");
    }

    private void configureBackpacks() {

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "backpacks.yml"));
        backpackService.configure(new BackpackConfigParser(message -> getLogger().warning("backpacks.yml: " + message))
                .parse(yaml));
        backpackRecipes.register();
        getServer().getOnlinePlayers().forEach(backpackRecipes::discover);
    }

    private void loadAndWire() {

        ExtrasActionExecutor actionExecutor = new ExtrasActionExecutor(this);

        activityStateResolver = new ActivityStateResolver();
        getServer().getPluginManager().registerEvents(activityStateResolver, this);
        getServer().getPluginManager().registerEvents(new CombatTagListener(activityStateResolver), this);

        RateConditionEvaluator rateConditionEvaluator = new RateConditionEvaluator(activityStateResolver);

        statManager = new StatManager(this);
        statManager.initialize();
        statEngine = new StatEngine(this, statManager, rateConditionEvaluator, actionExecutor);

        conditionManager = new ConditionManager(this);
        conditionManager.initialize();
        conditionRuntime = new ConditionRuntime(actionExecutor);
        conditionRuntime.start(this);

        statEngine.linkConditions(conditionManager, conditionRuntime);

        modifierManager = new ModifierManager(this);
        modifierManager.initialize();
        ModifierResolver modifierResolver = new ModifierResolver(modifierManager);
        statEngine.linkModifiers(modifierResolver);
        statEngine.linkAfkPolicy(AfkPolicy.from(getConfig()));

        statEngine.start();

        AmbientTemperatureCalculator ambientCalculator = new AmbientTemperatureCalculator();
        ThermalProtectionService thermalProtectionService = new ThermalProtectionService();
        TemperatureSettings temperatureSettings = new TemperatureSettingsLoader().load(this);
        temperatureEngine = new BodyTemperatureEngine(
                this, ambientCalculator, thermalProtectionService, actionExecutor, temperatureSettings);
        temperatureEngine.start();

        getServer().getPluginManager().registerEvents(new ConsumptionListener(statEngine), this);
        getServer().getPluginManager().registerEvents(
                new PlayerLifecycleListener(statEngine, conditionRuntime, temperatureEngine, activityStateResolver), this);

        HudSettings hudSettings = new HudSettingsLoader().load(this);
        hudEngine = new HudEngine(this, statManager, statEngine, hudSettings);
        hudEngine.start();

        NeedsService needsService = new NeedsService(statEngine);
        StatesService statesService = new StatesService(conditionManager, conditionRuntime);
        ExtrasAPI.init(needsService, statesService, temperatureEngine);

        TabIntegration.registerIfPresent(this, statManager, statEngine, temperatureEngine, conditionRuntime);
    }

    private void reload() {

        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));

        statEngine.stop();
        temperatureEngine.stop();
        hudEngine.stop();
        conditionRuntime.stop();

        statManager.reload();
        conditionManager.reload();
        modifierManager.reload();
        menuManager.reload();
        serverMenu.configure(ServerMenuConfig.from(getConfig().getConfigurationSection("server-menu")));
        // Quien ya está dentro recibe (o pierde) la brújula sin tener que reconectar.
        getServer().getOnlinePlayers().forEach(player -> {
            if (serverMenu.config().enabled()) {
                serverMenu.give(player);
            } else {
                serverMenu.remove(player);
            }
        });
        statEngine.linkAfkPolicy(AfkPolicy.from(getConfig()));
        // Las mochilas abiertas se guardan y cierran: su nivel pudo cambiar de tamaño.
        backpackService.closeAll();
        configureBackpacks();

        statEngine.start();
        conditionRuntime.start(this);
        temperatureEngine.start();
        hudEngine.start();
    }

    private void registerCommand() {

        var executor = new ExtrasAdminCommand(
                statManager, statEngine, conditionManager, conditionRuntime, this::reload, langManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "extrasadmin",
                "Gestiona needs y conditions de RPGRoll-Extras", "rpgrollextras.admin.*", executor);

        var menuExecutor = new MenuCommand(serverMenu, menuManager, langManager);
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "menu", "Menú del servidor",
                java.util.List.of(), menuExecutor, menuExecutor, "rpgrollextras.menu");

        var backpackExecutor = new BackpackCommand(backpackService, langManager);
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "mochila", "Entrega y lista mochilas",
                List.of("backpack"), backpackExecutor, backpackExecutor, BackpackCommand.ADMIN_PERMISSION);
    }

    @Override
    public void onDisable() {

        if (backpackService != null) {
            backpackService.closeAll();
        }

        if (statEngine != null) {
            // Un apagado no dispara PlayerQuitEvent a tiempo para todos.
            statEngine.saveAll();
            statEngine.stop();
        }

        if (conditionRuntime != null) {
            conditionRuntime.stop();
        }

        if (temperatureEngine != null) {
            temperatureEngine.stop();
        }

        if (hudEngine != null) {
            hudEngine.stop();
        }
    }

}

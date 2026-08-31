package com.sack.rpgroll.extras;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.extras.action.ExtrasActionExecutor;
import com.sack.rpgroll.extras.activity.ActivityStateResolver;
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

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ExtrasPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("stats", "conditions", "modifiers");

    private LangManager langManager;
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
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);
        new ResourceCopier(this).copyFiles(List.of(
                new com.sack.rpgroll.common.resource.ResourceFile("temperature.yml", "temperature.yml", false),
                new com.sack.rpgroll.common.resource.ResourceFile("hud.yml", "hud.yml", false)));

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        loadAndWire();

        registerCommand();

        getLogger().info("✔ RPGRoll-Extras habilitado. " + statManager.count() + " stat(s), "
                + conditionManager.count() + " condition(s), " + modifierManager.count() + " modificador(es).");
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

        statEngine.start();
        conditionRuntime.start(this);
        temperatureEngine.start();
        hudEngine.start();
    }

    private void registerCommand() {

        var command = getCommand("extrasadmin");

        if (command == null) {
            getLogger().severe("✘ El comando 'extrasadmin' no está declarado en plugin.yml");
            return;
        }

        var executor = new ExtrasAdminCommand(
                statManager, statEngine, conditionManager, conditionRuntime, this::reload, langManager);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    @Override
    public void onDisable() {

        if (statEngine != null) {
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

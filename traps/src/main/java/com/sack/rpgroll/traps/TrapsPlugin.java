package com.sack.rpgroll.traps;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.traps.command.TrapAdminCommand;
import com.sack.rpgroll.traps.condition.TrapConditionEvaluator;
import com.sack.rpgroll.traps.condition.TrapConditionRegistry;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.engine.TrapEngine;
import com.sack.rpgroll.traps.gui.ChatPromptManager;
import com.sack.rpgroll.traps.listener.TrapBlockProtectionListener;
import com.sack.rpgroll.traps.listener.TurretPlacementListener;
import com.sack.rpgroll.traps.listener.TrapTriggerListener;
import com.sack.rpgroll.traps.location.PlacedTrapManager;
import com.sack.rpgroll.traps.registry.BuiltinTrapActions;
import com.sack.rpgroll.traps.registry.TrapActionRegistry;
import com.sack.rpgroll.traps.turret.PlacedTurretManager;
import com.sack.rpgroll.traps.turret.TurretEngine;
import com.sack.rpgroll.traps.turret.TurretManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class TrapsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("traps", "turrets");
    private static final long DEFAULT_TICK_INTERVAL = 20L;
    private static final long DEFAULT_TURRET_TICK_INTERVAL = 5L;

    private TrapManager trapManager;
    private PlacedTrapManager placedTrapManager;
    private TrapEngine engine;
    private TurretManager turretManager;
    private PlacedTurretManager placedTurretManager;
    private TurretEngine turretEngine;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        trapManager = new TrapManager(this);
        trapManager.initialize();

        placedTrapManager = new PlacedTrapManager(this);
        placedTrapManager.load();

        TrapActionRegistry actionRegistry = new TrapActionRegistry(this);
        BuiltinTrapActions.registerAll(actionRegistry, this);

        TrapConditionRegistry conditionRegistry = new TrapConditionRegistry();
        TrapConditionEvaluator conditionEvaluator = new TrapConditionEvaluator(conditionRegistry);

        engine = new TrapEngine(this, trapManager, placedTrapManager, actionRegistry, conditionEvaluator);

        long tickInterval = getConfig().getLong("engine.tick-interval-ticks", DEFAULT_TICK_INTERVAL);
        engine.start(tickInterval);

        turretManager = new TurretManager(this);
        turretManager.initialize();

        placedTurretManager = new PlacedTurretManager(this);
        placedTurretManager.load();

        turretEngine = new TurretEngine(this, turretManager, placedTurretManager, conditionEvaluator, actionRegistry);

        long turretTickInterval = getConfig().getLong("engine.turret-tick-interval-ticks",
                DEFAULT_TURRET_TICK_INTERVAL);
        turretEngine.start(turretTickInterval);

        getServer().getPluginManager().registerEvents(
                new TrapTriggerListener(trapManager, placedTrapManager, engine), this);
        getServer().getPluginManager().registerEvents(
                new TrapBlockProtectionListener(trapManager, placedTrapManager, langManager), this);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        // Coloca/retira torretas como bloques; solo el dueño puede sacarlas.
        getServer().getPluginManager().registerEvents(
                new TurretPlacementListener(this, turretManager, placedTurretManager, turretEngine, langManager),
                this);

        var trapAdminCommand = getCommand("trapadmin");
        if (trapAdminCommand == null) {
            getLogger().severe("✘ El comando 'trapadmin' no está declarado en plugin.yml");
        } else {
            var executor = new TrapAdminCommand(this, trapManager, placedTrapManager, engine, turretManager,
                    placedTurretManager, turretEngine, chatPromptManager, langManager);
            trapAdminCommand.setExecutor(executor);
            trapAdminCommand.setTabCompleter(executor);
        }

        getLogger().info("✔ RPGRoll-Traps habilitado. " + trapManager.count() + " tipo(s) de trampa cargados, "
                + placedTrapManager.getAll().size() + " instancia(s) colocada(s), " + turretManager.count()
                + " tipo(s) de torreta cargados, " + placedTurretManager.getAll().size() + " colocada(s).");
    }

    public TrapManager getTrapManager() {
        return trapManager;
    }

    public PlacedTrapManager getPlacedTrapManager() {
        return placedTrapManager;
    }

    public TrapEngine getEngine() {
        return engine;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public TurretManager getTurretManager() {
        return turretManager;
    }

    public PlacedTurretManager getPlacedTurretManager() {
        return placedTurretManager;
    }

    public TurretEngine getTurretEngine() {
        return turretEngine;
    }

}

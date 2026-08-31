package com.sack.rpgroll.mobs;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.assets.ModuleAssetSync;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.mobs.command.MobAdminCommand;
import com.sack.rpgroll.mobs.condition.MobConditionEvaluator;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.engine.MobEngine;
import com.sack.rpgroll.mobs.gui.ChatPromptManager;
import com.sack.rpgroll.mobs.instance.MobInstanceService;
import com.sack.rpgroll.mobs.integration.MobsPlaceholders;
import com.sack.rpgroll.mobs.listener.MobAITask;
import com.sack.rpgroll.mobs.listener.MobCombatListener;
import com.sack.rpgroll.mobs.listener.NaturalSpawnListener;
import com.sack.rpgroll.mobs.rarity.MobRarityResolver;
import com.sack.rpgroll.mobs.region.MobRegionManager;
import com.sack.rpgroll.mobs.registry.ActionRegistry;
import com.sack.rpgroll.mobs.registry.BuiltinMobActions;
import com.sack.rpgroll.mobs.registry.ConditionRegistry;
import com.sack.rpgroll.mobs.registry.MobStatRegistry;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MobsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("mobs", "regions");
    private static final long AI_TICK_INTERVAL = 10L;

    private MobManager mobManager;
    private MobRegionManager regionManager;
    private MobInstanceService instanceService;
    private MobStatRegistry statRegistry;
    private ActionRegistry actionRegistry;
    private ConditionRegistry conditionRegistry;
    private MobConditionEvaluator conditionEvaluator;
    private MobRarityResolver rarityResolver;
    private MobEngine engine;
    private ChatPromptManager chatPromptManager;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        mobManager = new MobManager(this);
        regionManager = new MobRegionManager(this);

        mobManager.initialize();
        regionManager.initialize();

        instanceService = new MobInstanceService(this);
        statRegistry = new MobStatRegistry();
        conditionRegistry = new ConditionRegistry();
        conditionEvaluator = new MobConditionEvaluator(conditionRegistry);
        rarityResolver = new MobRarityResolver();

        actionRegistry = new ActionRegistry(this);
        BuiltinMobActions.registerAll(actionRegistry, this);

        engine = new MobEngine(this, mobManager, instanceService, actionRegistry, conditionEvaluator, rarityResolver);

        new ModuleAssetSync(this, "mobs").syncAll();

        chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        MobCombatListener combatListener = new MobCombatListener(engine, instanceService, mobManager, this);
        getServer().getPluginManager().registerEvents(combatListener, this);

        NaturalSpawnListener spawnListener = new NaturalSpawnListener(mobManager, engine, regionManager);
        getServer().getPluginManager().registerEvents(spawnListener, this);

        MobAITask aiTask = new MobAITask(engine, instanceService, regionManager, mobManager);
        getServer().getScheduler().runTaskTimer(this, aiTask, AI_TICK_INTERVAL, AI_TICK_INTERVAL);

        registerCommand("mobadmin",
                new MobAdminCommand(mobManager, engine, statRegistry, chatPromptManager, this));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Mobs habilitado. " + mobManager.count() + " mob(s), "
                + regionManager.count() + " región(es) cargadas.");
    }

    private void registerPlaceholders() {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new MobsPlaceholders(this, mobManager, engine).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollmobs_...%)");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {

        var command = getCommand(name);

        if (command == null) {
            getLogger().severe("✘ El comando '" + name + "' no está declarado en plugin.yml");
            return;
        }

        command.setExecutor(executor);


        if (executor instanceof org.bukkit.command.TabCompleter tabCompleter) {

            command.setTabCompleter(tabCompleter);

        }
    }

    // ============ API pública para otros addons ============

    public MobManager getMobManager() {
        return mobManager;
    }

    public MobRegionManager getRegionManager() {
        return regionManager;
    }

    public MobInstanceService getInstanceService() {
        return instanceService;
    }

    public MobStatRegistry getStatRegistry() {
        return statRegistry;
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public ConditionRegistry getConditionRegistry() {
        return conditionRegistry;
    }

    public MobEngine getEngine() {
        return engine;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}

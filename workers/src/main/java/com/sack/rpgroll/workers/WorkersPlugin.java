package com.sack.rpgroll.workers;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.assets.ModuleAssetSync;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.workers.api.WorkersAPI;
import com.sack.rpgroll.workers.command.WorkersAdminCommand;
import com.sack.rpgroll.workers.command.WorkersCommand;
import com.sack.rpgroll.workers.core.ai.NeedsTask;
import com.sack.rpgroll.workers.core.ai.WorkerAiTask;
import com.sack.rpgroll.workers.core.behavior.FarmerBehavior;
import com.sack.rpgroll.workers.core.behavior.FishingBehavior;
import com.sack.rpgroll.workers.core.behavior.LumberjackBehavior;
import com.sack.rpgroll.workers.core.behavior.MinerBehavior;
import com.sack.rpgroll.workers.core.behavior.ProfessionBehaviorRegistry;
import com.sack.rpgroll.workers.core.behavior.RanchingBehavior;
import com.sack.rpgroll.workers.core.economy.EconomyService;
import com.sack.rpgroll.workers.core.economy.MoraleEngine;
import com.sack.rpgroll.workers.core.economy.WageTask;
import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.event.WorkerEventTask;
import com.sack.rpgroll.workers.core.logistics.WarehouseManager;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.workers.core.skill.SkillManager;
import com.sack.rpgroll.workers.core.worker.WorkerManager;
import com.sack.rpgroll.workers.gui.ChatPromptManager;
import com.sack.rpgroll.workers.listener.WarehouseDesignatorListener;
import com.sack.rpgroll.workers.listener.WorkerDeathListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Punto de entrada de RPGRoll-Workers. Cablea los 4 managers de
 * contenido, el registro de workers, las 4 tareas periódicas (IA/
 * necesidades/salarios/eventos), la logística de almacenes, y los
 * behaviors incorporados (minero/granjero/leñador/ganadero/pescador).
 */
public class WorkersPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("professions", "skills", "schedules", "worker-events",
            "workers");

    private ProfessionManager professionManager;
    private SkillManager skillManager;
    private ScheduleManager scheduleManager;
    private WorkerEventManager workerEventManager;
    private WorkerManager workerManager;
    private WarehouseManager warehouseManager;
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

        new ModuleAssetSync(this, "workers").syncAll();

        initializeManagers();

        warehouseManager = new WarehouseManager(this);
        warehouseManager.load();

        workerManager = new WorkerManager(this);
        workerManager.loadAll();

        ProfessionBehaviorRegistry behaviorRegistry = new ProfessionBehaviorRegistry();
        double workSearchRadius = getConfig().getDouble("work-search-radius", 16);
        behaviorRegistry.register("miner", new MinerBehavior(workSearchRadius));
        behaviorRegistry.register("farmer", new FarmerBehavior(workSearchRadius));
        behaviorRegistry.register("lumberjack", new LumberjackBehavior(workSearchRadius));
        behaviorRegistry.register("rancher", new RanchingBehavior(workSearchRadius, "hay"));
        behaviorRegistry.register("fisherman", new FishingBehavior(workSearchRadius));

        EconomyService economyService = new EconomyService();
        MoraleEngine moraleEngine = new MoraleEngine();

        WorkersAPI.init(professionManager, skillManager, scheduleManager, workerEventManager, workerManager,
                warehouseManager, behaviorRegistry);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);
        getServer().getPluginManager().registerEvents(new WarehouseDesignatorListener(warehouseManager, langManager), this);
        getServer().getPluginManager().registerEvents(new WorkerDeathListener(workerManager), this);

        startTasks(behaviorRegistry, economyService, moraleEngine, workSearchRadius);

        var adminCommand = getCommand("workersadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'workersadmin' no está declarado en plugin.yml");
        } else {
            var workersAdminCommand = new WorkersAdminCommand(professionManager, skillManager, scheduleManager,
                    workerEventManager, workerManager, warehouseManager, chatPromptManager, this::reloadContent);
            adminCommand.setExecutor(workersAdminCommand);
            adminCommand.setTabCompleter(workersAdminCommand);
        }

        var playerCommand = getCommand("workers");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'workers' no está declarado en plugin.yml");
        } else {
            var workersCommand = new WorkersCommand(workerManager, professionManager, chatPromptManager);
            playerCommand.setExecutor(workersCommand);
            playerCommand.setTabCompleter(workersCommand);
        }

        getLogger().info("✔ RPGRoll-Workers habilitado. " + professionManager.count() + " profesión(es), "
                + workerManager.getAll().size() + " worker(s) cargado(s).");
    }

    @Override
    public void onDisable() {

        if (workerManager != null) {
            workerManager.saveAll();
        }

        if (warehouseManager != null) {
            warehouseManager.save();
        }
    }

    private void initializeManagers() {

        professionManager = new ProfessionManager(this);
        professionManager.initialize();

        skillManager = new SkillManager(this);
        skillManager.initialize();

        scheduleManager = new ScheduleManager(this);
        scheduleManager.initialize();

        workerEventManager = new WorkerEventManager(this);
        workerEventManager.initialize();
    }

    private void reloadContent() {
        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));
        professionManager.reload();
        skillManager.reload();
        scheduleManager.reload();
        workerEventManager.reload();
    }

    private void startTasks(ProfessionBehaviorRegistry behaviorRegistry, EconomyService economyService,
            MoraleEngine moraleEngine, double workSearchRadius) {

        long needsInterval = getConfig().getLong("needs-decay-interval-ticks", 200);
        long aiInterval = getConfig().getLong("ai-tick-interval-ticks", 40);
        long eventInterval = getConfig().getLong("worker-event-check-interval-ticks", 1200);
        long wageInterval = getConfig().getLong("wage-payment-interval-ticks", 24000);
        double warehouseSearchRadius = getConfig().getDouble("warehouse-search-radius", 32);

        double hungerDecay = getConfig().getDouble("hunger-decay-per-check", 1.0);
        double energyDecay = getConfig().getDouble("energy-decay-per-check", 0.6);
        double sleepDecay = getConfig().getDouble("sleep-decay-per-check", 0.4);
        double stressGain = getConfig().getDouble("stress-gain-per-check", 0.3);

        double eventBaseChance = getConfig().getDouble("worker-event-base-chance", 0.01);
        double lowMoraleThreshold = getConfig().getDouble("low-morale-threshold", 30);

        new NeedsTask(workerManager, hungerDecay, energyDecay, sleepDecay, stressGain)
                .runTaskTimer(this, needsInterval, needsInterval);

        new WorkerAiTask(workerManager, professionManager, scheduleManager, warehouseManager, behaviorRegistry,
                economyService, moraleEngine, warehouseSearchRadius).runTaskTimer(this, aiInterval, aiInterval);

        new WageTask(workerManager, economyService, moraleEngine, wageInterval, langManager).runTaskTimer(this, wageInterval,
                wageInterval);

        new WorkerEventTask(workerManager, workerEventManager, eventInterval, eventBaseChance, lowMoraleThreshold,
                langManager).runTaskTimer(this, eventInterval, eventInterval);
    }

    public ProfessionManager getProfessionManager() {
        return professionManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public ScheduleManager getScheduleManager() {
        return scheduleManager;
    }

    public WorkerEventManager getWorkerEventManager() {
        return workerEventManager;
    }

    public WorkerManager getWorkerManager() {
        return workerManager;
    }

    public WarehouseManager getWarehouseManager() {
        return warehouseManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}

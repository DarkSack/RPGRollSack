package com.sack.rpgroll.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.CommandManager;
import com.sack.rpgroll.command.commands.*;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.content.Reloadable;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.gameplay.listener.LevelUpListener;
import com.sack.rpgroll.gameplay.listener.MobKillListener;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantedBookFactory;
import com.sack.rpgroll.gameplay.enchant.ItemEnchantmentStorage;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectRegistry;
import com.sack.rpgroll.gameplay.enchant.effect.handlers.LifestealEffectHandler;
import com.sack.rpgroll.gameplay.enchant.effect.handlers.PotionEffectSelfPassiveHandler;
import com.sack.rpgroll.gameplay.enchant.effect.handlers.PotionEffectTargetHandler;
import com.sack.rpgroll.gameplay.enchant.effect.handlers.SetFireEffectHandler;
import com.sack.rpgroll.gameplay.enchant.listener.EnchantCombiner;
import com.sack.rpgroll.gameplay.enchant.listener.EnchantDropListener;
import com.sack.rpgroll.gameplay.enchant.listener.EnchantHitListener;
import com.sack.rpgroll.gameplay.enchant.listener.EnchantShopListener;
import com.sack.rpgroll.gameplay.enchant.listener.PassiveEnchantTask;
import com.sack.rpgroll.gameplay.job.ExplorerProgressStorage;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gameplay.job.JobRewardService;
import com.sack.rpgroll.gameplay.job.PlacedBlockCleanupTask;
import com.sack.rpgroll.gameplay.job.PlacedBlockTracker;
import com.sack.rpgroll.gameplay.job.listener.AlquimistaJobListener;
import com.sack.rpgroll.gameplay.job.listener.CazadorJobListener;
import com.sack.rpgroll.gameplay.job.listener.ExplorerJobListener;
import com.sack.rpgroll.gameplay.job.listener.GranjeroJobListener;
import com.sack.rpgroll.gameplay.job.listener.MinerJobListener;
import com.sack.rpgroll.gameplay.job.listener.PescadorJobListener;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.skill.SkillManager;
import com.sack.rpgroll.gameplay.trait.TraitManager;
import com.sack.rpgroll.gui.listener.GUIListener;
import com.sack.rpgroll.integration.VaultEconomyProvider;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.listener.PlayerEventListener;
import com.sack.rpgroll.playerclass.ClassManager;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.race.RaceManager;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

public class Bootstrap {

    private final RPGRoll plugin;
    private final ServiceRegistry services;

    public Bootstrap(RPGRoll plugin) {
        this.plugin = plugin;
        this.services = new ServiceRegistry();
    }

    public void initialize() {

        try {
            printBanner();
            registerCoreServices();
            registerCommands();
            registerEventListeners();

            plugin.getLogger().info("==================================");
            plugin.getLogger().info("RPGRoll iniciado correctamente.");
            plugin.getLogger().info("==================================");

        } catch (Exception e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "✘ Error crítico durante el arranque de RPGRoll",
                    e);

            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public void shutdown() {

        plugin.getLogger().info("Deteniendo servicios...");

        // Guardar todos los jugadores
        if (services.contains(PlayerManager.class)) {

            PlayerManager playerManager = services.get(PlayerManager.class);
            playerManager.saveAll();

        }

        // Cerrar base de datos
        if (services.contains(DatabaseManager.class)) {

            DatabaseManager dbManager = services.get(DatabaseManager.class);
            dbManager.shutdown();

        }

        if (services.contains(PassiveEnchantTask.class)) {
            services.get(PassiveEnchantTask.class).cancel();
        }

        if (services.contains(PlacedBlockCleanupTask.class)) {
            services.get(PlacedBlockCleanupTask.class).cancel();
        }

        plugin.getLogger().info("✔ Todos los servicios detenidos correctamente.");

    }

    private final List<Reloadable> reloadableContent = new ArrayList<>();

    private void registerCoreServices() {

        plugin.getLogger().info("Inicializando servicios...");

        // 1. ConfigManager - Gestión de configuración
        ConfigManager configManager = new ConfigManager(plugin);
        configManager.initialize();
        services.register(ConfigManager.class, configManager);
        plugin.getLogger().info("✔ ConfigManager registrado");

        // 2. DatabaseManager - Gestión de base de datos
        DatabaseManager dbManager = new DatabaseManager(plugin);
        dbManager.initialize();
        services.register(DatabaseManager.class, dbManager);
        plugin.getLogger().info("✔ DatabaseManager registrado");

        // 3. PlayerManager - Gestión de jugadores RPG
        PlayerManager playerManager = new PlayerManager(plugin, dbManager);
        services.register(PlayerManager.class, playerManager);
        plugin.getLogger().info("✔ PlayerManager registrado");

        // 4. SkillManager - Carga de habilidades
        SkillManager skillManager = new SkillManager(plugin, configManager.getYamlLoader());
        skillManager.initialize();
        services.register(SkillManager.class, skillManager);
        reloadableContent.add(skillManager);
        plugin.getLogger().info("✔ SkillManager registrado");

        // 5. TraitManager - Carga de traits
        TraitManager traitManager = new TraitManager(plugin, configManager.getYamlLoader());
        traitManager.initialize();
        services.register(TraitManager.class, traitManager);
        reloadableContent.add(traitManager);
        plugin.getLogger().info("✔ TraitManager registrado");

        // 6. LevelUpRewardsConfig - Carga de recompensas de level up
        LevelUpRewardsConfig levelUpRewardsConfig = new LevelUpRewardsConfig(plugin, configManager.getYamlLoader());
        levelUpRewardsConfig.load();
        services.register(LevelUpRewardsConfig.class, levelUpRewardsConfig);
        plugin.getLogger().info("✔ LevelUpRewardsConfig registrado");

        // 7. RaceManager - Sistema de razas
        RaceManager raceManager = new RaceManager(plugin, configManager.getYamlLoader());
        raceManager.initialize();
        services.register(RaceManager.class, raceManager);
        reloadableContent.add(raceManager);
        plugin.getLogger().info("✔ RaceManager registrado");

        // 7.1 RaceAttributeApplier - Aplicador de atributos de raza
        RaceAttributeApplier raceAttributeApplier = new RaceAttributeApplier(plugin);
        services.register(RaceAttributeApplier.class, raceAttributeApplier);
        plugin.getLogger().info("✔ RaceAttributeApplier registrado");

        // 8. ClassManager - Sistema de clases
        ClassManager classManager = new ClassManager(plugin, configManager.getYamlLoader());
        classManager.initialize();
        services.register(ClassManager.class, classManager);
        reloadableContent.add(classManager);
        plugin.getLogger().info("✔ ClassManager registrado");

        // 9. PlacedBlockTracker - Sistema anti-farm para minero
        PlacedBlockTracker placedBlockTracker = new PlacedBlockTracker(plugin, dbManager);
        services.register(PlacedBlockTracker.class, placedBlockTracker);
        plugin.getLogger().info("✔ PlacedBlockTracker registrado");

        // 10. VaultEconomyProvider - Conexión con Vault (opcional)
        VaultEconomyProvider economyProvider = new VaultEconomyProvider(plugin);
        boolean economyAvailable = economyProvider.setup();
        services.register(VaultEconomyProvider.class, economyProvider);

        // 11. JobManager - Sistema de trabajos
        JobManager jobManager = new JobManager(plugin, configManager.getYamlLoader());
        jobManager.initialize();
        services.register(JobManager.class, jobManager);
        reloadableContent.add(jobManager);
        plugin.getLogger().info("✔ JobManager registrado");

        // Explorer Progress - Sistema de progreso de exploración
        ExplorerProgressStorage explorerProgressStorage = new ExplorerProgressStorage(plugin, dbManager);
        services.register(ExplorerProgressStorage.class, explorerProgressStorage);

        PlacedBlockCleanupTask cleanupTask = new PlacedBlockCleanupTask(plugin, dbManager);
        cleanupTask.start();
        services.register(PlacedBlockCleanupTask.class, cleanupTask);

        if (!economyAvailable) {
            plugin.getLogger().warning(
                    "✘ Sistema de trabajos cargado, pero sin economía activa. Las recompensas en dinero no se pagarán.");
        }

        // 12. Sistema de encantamientos - orden correcto: catálogo -> storage ->
        // factory -> efectos
        EnchantManager enchantManager = new EnchantManager(plugin, configManager.getYamlLoader());
        enchantManager.initialize();
        services.register(EnchantManager.class, enchantManager);
        reloadableContent.add(enchantManager);
        plugin.getLogger().info("✔ EnchantManager registrado");

        ItemEnchantmentStorage itemEnchantmentStorage = new ItemEnchantmentStorage(plugin, enchantManager);
        services.register(ItemEnchantmentStorage.class, itemEnchantmentStorage);

        EnchantedBookFactory enchantedBookFactory = new EnchantedBookFactory(itemEnchantmentStorage);
        services.register(EnchantedBookFactory.class, enchantedBookFactory);

        EnchantEffectRegistry enchantEffectRegistry = new EnchantEffectRegistry();
        enchantEffectRegistry.register("SET_FIRE", new SetFireEffectHandler());
        enchantEffectRegistry.register("LIFESTEAL", new LifestealEffectHandler());
        enchantEffectRegistry.register("POTION_EFFECT_TARGET", new PotionEffectTargetHandler());
        enchantEffectRegistry.register("POTION_EFFECT_SELF_PASSIVE", new PotionEffectSelfPassiveHandler());
        services.register(EnchantEffectRegistry.class, enchantEffectRegistry);
        plugin.getLogger().info("✔ EnchantEffectRegistry registrado (4 tipos de efecto)");

    }

    public List<Reloadable> getReloadableContent() {
        return Collections.unmodifiableList(reloadableContent);
    }

    private void registerCommands() {

        plugin.getLogger().info("Registrando comandos...");

        CommandManager commandManager = new CommandManager(plugin);

        // Registrar subcomandos
        commandManager.register(new StatsCommand(plugin));
        commandManager.register(new LevelCommand(plugin));
        commandManager.register(new ClassCommand(plugin));
        commandManager.register(new RaceCommand(plugin));
        commandManager.register(new CreateCommand(plugin));
        commandManager.register(new SkillsCommand(plugin));
        commandManager.register(new TraitsCommand(plugin));
        commandManager.register(new AddXPCommand(plugin));
        commandManager.register(new MyStatsCommand(plugin));
        commandManager.register(new LevelUpDebugCommand(plugin));
        commandManager.register(new AdminGuiCommand(plugin));
        commandManager.register(new ReloadCommand(plugin));
        commandManager.register(new JobsCommand(plugin));
        commandManager.register(new AdminSetRaceCommand(plugin));
        commandManager.register(new AdminSetClassCommand(plugin));
        commandManager.register(new EnchantCommand(plugin));
        commandManager.register(new AdminJobCommand(plugin));

        // Registrar el comando principal /rpg
        plugin.getCommand("rpg").setExecutor(commandManager);
        plugin.getCommand("rpg").setTabCompleter(commandManager);

        services.register(CommandManager.class, commandManager);

        plugin.getLogger().info("✔ Sistema de comandos inicializado.");
        plugin.getLogger().info("");

    }

    private void registerEventListeners() {

        plugin.getLogger().info("Registrando event listeners...");

        // Servicios base que varios listeners necesitan
        PlayerManager playerManager = services.get(PlayerManager.class);
        RaceManager raceManager = services.get(RaceManager.class);
        ClassManager classManager = services.get(ClassManager.class);
        JobManager jobManager = services.get(JobManager.class);
        VaultEconomyProvider economyProvider = services.get(VaultEconomyProvider.class);
        ConfigManager configManager = services.get(ConfigManager.class);
        LevelUpRewardsConfig levelUpRewardsConfig = services.get(LevelUpRewardsConfig.class);
        PlacedBlockTracker placedBlockTracker = services.get(PlacedBlockTracker.class);
        EnchantManager enchantManager = services.get(EnchantManager.class);
        ItemEnchantmentStorage itemEnchantmentStorage = services.get(ItemEnchantmentStorage.class);
        EnchantedBookFactory enchantedBookFactory = services.get(EnchantedBookFactory.class);
        EnchantEffectRegistry enchantEffectRegistry = services.get(EnchantEffectRegistry.class);

        // Clave compartida para marcar entidades nacidas de spawner (anti-farm
        // de Cazador y anti-drop-de-encantamientos en granjas automáticas)
        NamespacedKey fromSpawnerKey = new NamespacedKey(plugin, "from-spawner");

        // ===== Listeners de trabajos =====
        JobRewardService jobRewardService = new JobRewardService(plugin, playerManager, jobManager, economyProvider);

        MinerJobListener minerJobListener = new MinerJobListener(jobRewardService, placedBlockTracker);
        Bukkit.getPluginManager().registerEvents(minerJobListener, plugin);

        PescadorJobListener pescadorJobListener = new PescadorJobListener(jobRewardService);
        Bukkit.getPluginManager().registerEvents(pescadorJobListener, plugin);

        CazadorJobListener cazadorJobListener = new CazadorJobListener(jobRewardService, fromSpawnerKey);
        Bukkit.getPluginManager().registerEvents(cazadorJobListener, plugin);

        GranjeroJobListener granjeroJobListener = new GranjeroJobListener(jobRewardService);
        Bukkit.getPluginManager().registerEvents(granjeroJobListener, plugin);

        AlquimistaJobListener alquimistaJobListener = new AlquimistaJobListener(jobRewardService);
        Bukkit.getPluginManager().registerEvents(alquimistaJobListener, plugin);

        // ===== Listeners de jugador =====
        RaceAttributeApplier raceAttributeApplier = services.get(RaceAttributeApplier.class);
        PlayerEventListener playerListener = new PlayerEventListener(plugin, playerManager, raceManager, classManager,
                raceAttributeApplier);
        Bukkit.getPluginManager().registerEvents(playerListener, plugin);

        // ===== Listeners de GUI =====
        GUIListener guiListener = new GUIListener(plugin);
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);

        // ===== Listeners de gameplay (XP, skills, etc) =====
        MobKillListener mobKillListener = new MobKillListener(playerManager, configManager, levelUpRewardsConfig);
        Bukkit.getPluginManager().registerEvents(mobKillListener, plugin);

        LevelUpListener levelUpListener = new LevelUpListener();
        Bukkit.getPluginManager().registerEvents(levelUpListener, plugin);

        // ===== Listeners de explorador =====
        ExplorerProgressStorage explorerProgressStorage = services.get(ExplorerProgressStorage.class);
        ExplorerJobListener explorerJobListener = new ExplorerJobListener(jobManager, playerManager,
                explorerProgressStorage, jobRewardService);
        Bukkit.getPluginManager().registerEvents(explorerJobListener, plugin);

        // ===== Listeners de encantamientos =====
        EnchantHitListener enchantHitListener = new EnchantHitListener(itemEnchantmentStorage, enchantManager,
                enchantEffectRegistry);
        Bukkit.getPluginManager().registerEvents(enchantHitListener, plugin);

        EnchantCombiner enchantCombiner = new EnchantCombiner(itemEnchantmentStorage, enchantManager);
        Bukkit.getPluginManager().registerEvents(enchantCombiner, plugin);

        EnchantDropListener enchantDropListener = new EnchantDropListener(enchantManager, enchantedBookFactory,
                fromSpawnerKey);
        Bukkit.getPluginManager().registerEvents(enchantDropListener, plugin);

        EnchantShopListener enchantShopListener = new EnchantShopListener(enchantManager, enchantedBookFactory,
                economyProvider);
        Bukkit.getPluginManager().registerEvents(enchantShopListener, plugin);

        PassiveEnchantTask passiveEnchantTask = new PassiveEnchantTask(itemEnchantmentStorage, enchantManager,
                enchantEffectRegistry);
        passiveEnchantTask.start(plugin);
        services.register(PassiveEnchantTask.class, passiveEnchantTask);

        plugin.getLogger().info("✔ Event listeners registrados.");

    }

    private void printBanner() {

        plugin.getLogger().info("");
        plugin.getLogger().info("==================================");
        plugin.getLogger().info("          RPGRoll");
        plugin.getLogger().info("     RPG Framework for Paper");
        plugin.getLogger().info("==================================");

    }

    public ServiceRegistry getServices() {
        return services;
    }

}
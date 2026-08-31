package com.sack.rpgroll.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.command.CommandManager;
import com.sack.rpgroll.command.commands.*;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.common.content.Reloadable;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.gameplay.combat.CombatTracker;
import com.sack.rpgroll.gameplay.combat.ResourceRegenTask;
import com.sack.rpgroll.gameplay.hud.PlayerResourceBar;
import com.sack.rpgroll.gameplay.listener.CombatEffectsListener;
import com.sack.rpgroll.gameplay.listener.LevelUpListener;
import com.sack.rpgroll.gameplay.listener.MobKillListener;
import com.sack.rpgroll.gameplay.skill.SkillCooldownTracker;
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
import com.sack.rpgroll.gui.admin.ChatPromptManager;
import com.sack.rpgroll.gui.listener.GUIListener;
import com.sack.rpgroll.integration.RPGRollPlaceholders;
import com.sack.rpgroll.integration.VaultEconomyProvider;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.listener.PlayerEventListener;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.playerclass.ClassManagerImpl;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.race.RaceManagerImpl;
import java.util.logging.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;

public class Bootstrap {

    private final RPGRoll plugin;
    private final ServiceRegistry services;

    public Bootstrap(RPGRoll plugin) {
        this.plugin = plugin;
        this.services = new ServiceRegistry();
    }

    public void initialize() {

        try {
            if (!verifyLicense()) {
                Bukkit.getPluginManager().disablePlugin(plugin);
                return;
            }

            printBanner();
            registerCoreServices();
            registerCommands();
            registerEventListeners();

            RPGRollAPI.init(plugin);
            plugin.getLogger().info("✔ RPGRollAPI inicializada");

            registerPlaceholders();

            plugin.getLogger().info("==================================");
            plugin.getLogger().info("RPGRoll iniciado correctamente.");
            plugin.getLogger().info("==================================");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "✘ Error crítico durante el arranque de RPGRoll", e);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * @return true si la licencia es válida y el arranque puede continuar.
     *         Si es false, ya se logueó el motivo — el caller solo debe
     *         deshabilitar el plugin.
     */
    private boolean verifyLicense() {
        return LicenseGate.verify(plugin, LicenseIdentity.RESOURCE_ID);
    }

    /**
     * PlaceholderAPI es softdepend — el framework debe arrancar igual sin
     * ella. Registrar la expansión acá (después de {@code RPGRollAPI.init})
     * asegura que %rpgroll_...% ya pueda leer jugadores desde el primer
     * placeholder resuelto.
     */
    private void registerPlaceholders() {

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new RPGRollPlaceholders(plugin).register();
        plugin.getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgroll_...%)");
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
        // Cancelar tareas programadas
        if (services.contains(PlacedBlockCleanupTask.class)) {
            services.get(PlacedBlockCleanupTask.class).cancel();
        }
        if (services.contains(ResourceRegenTask.class)) {
            services.get(ResourceRegenTask.class).cancel();
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

        // 1.1 LangManager - Mensajes por idioma (lang/es.yml, en.yml, pt_BR.yml)
        LangManager langManager = new LangManager(plugin, List.of("es", "en", "pt_BR"), "es");
        FileConfiguration mainConfig = configManager.getConfig("config.yml");
        String configuredLocale = mainConfig != null ? mainConfig.getString("language", "es") : "es";
        langManager.reload(configuredLocale);
        services.register(LangManager.class, langManager);
        plugin.getLogger().info("✔ LangManager registrado (idioma: " + configuredLocale + ")");

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
        RaceManagerImpl raceManager = new RaceManagerImpl(plugin, configManager.getYamlLoader());
        raceManager.initialize();
        services.register(RaceManager.class, raceManager);
        reloadableContent.add(raceManager);
        plugin.getLogger().info("✔ RaceManager registrado");

        // 7.1 RaceAttributeApplier - Aplicador de atributos de raza
        RaceAttributeApplier raceAttributeApplier = new RaceAttributeApplier(plugin);
        services.register(RaceAttributeApplier.class, raceAttributeApplier);
        plugin.getLogger().info("✔ RaceAttributeApplier registrado");

        // 8. ClassManager - Sistema de clases
        ClassManagerImpl classManager = new ClassManagerImpl(plugin, configManager.getYamlLoader());
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

        // 12. CombatTracker / SkillCooldownTracker / PlayerResourceBar - combate y HUD
        CombatTracker combatTracker = new CombatTracker();
        services.register(CombatTracker.class, combatTracker);

        SkillCooldownTracker skillCooldownTracker = new SkillCooldownTracker();
        services.register(SkillCooldownTracker.class, skillCooldownTracker);

        PlayerResourceBar resourceBar = new PlayerResourceBar(plugin);
        services.register(PlayerResourceBar.class, resourceBar);
        plugin.getLogger().info("✔ CombatTracker, SkillCooldownTracker y PlayerResourceBar registrados");

        // 13. ResourceRegenTask - regeneración pasiva de salud/maná
        FileConfiguration gameplayConfig = configManager.getConfig("gameplay.yml");
        double healthRegenPercent = gameplayConfig != null
                ? gameplayConfig.getDouble("combat.health_regen_percent", 2.0)
                : 2.0;
        double manaRegenPercent = gameplayConfig != null
                ? gameplayConfig.getDouble("combat.mana_regen_percent", 5.0)
                : 5.0;
        int regenIntervalSeconds = gameplayConfig != null
                ? gameplayConfig.getInt("combat.regen_interval_seconds", 5)
                : 5;
        int combatDurationSeconds = gameplayConfig != null
                ? gameplayConfig.getInt("combat.combat_duration", 10)
                : 10;
        boolean regenInCombat = gameplayConfig != null
                && gameplayConfig.getBoolean("combat.natural_regen_in_combat", false);

        ResourceRegenTask regenTask = new ResourceRegenTask(plugin, playerManager, combatTracker, resourceBar,
                healthRegenPercent, manaRegenPercent, combatDurationSeconds, regenInCombat);
        regenTask.start(regenIntervalSeconds * 20L);
        services.register(ResourceRegenTask.class, regenTask);
        plugin.getLogger().info("✔ ResourceRegenTask iniciada (cada " + regenIntervalSeconds + "s)");

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
        commandManager.register(new AdminJobCommand(plugin));
        commandManager.register(new AllocateStatCommand(plugin));
        commandManager.register(new UseSkillCommand(plugin));
        commandManager.register(new AdminResetStatsCommand(plugin));
        commandManager.register(new AdminContentCommand(plugin));

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
        LangManager langManager = services.get(LangManager.class);

        // Clave compartida para marcar entidades nacidas de spawner (anti-farm
        // de Cazador y anti-drop-de-encantamientos en granjas automáticas)
        NamespacedKey fromSpawnerKey = new NamespacedKey(plugin, "from-spawner");

        // ===== Listeners de trabajos =====
        JobRewardService jobRewardService = new JobRewardService(plugin, playerManager, jobManager, economyProvider,
                langManager);

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
        PlayerResourceBar resourceBar = services.get(PlayerResourceBar.class);
        PlayerEventListener playerListener = new PlayerEventListener(plugin, playerManager, raceManager, classManager,
                raceAttributeApplier, resourceBar, langManager);
        Bukkit.getPluginManager().registerEvents(playerListener, plugin);

        // ===== Listeners de combate (armadura, evasión, crítico, salud) =====
        CombatTracker combatTracker = services.get(CombatTracker.class);
        CombatEffectsListener combatEffectsListener = new CombatEffectsListener(playerManager, combatTracker,
                resourceBar, langManager);
        Bukkit.getPluginManager().registerEvents(combatEffectsListener, plugin);

        // ===== Listeners de GUI =====
        GUIListener guiListener = new GUIListener(plugin);
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);

        ChatPromptManager chatPromptManager = new ChatPromptManager(plugin, langManager);
        Bukkit.getPluginManager().registerEvents(chatPromptManager, plugin);
        services.register(ChatPromptManager.class, chatPromptManager);

        // ===== Listeners de gameplay (XP, skills, etc) =====
        MobKillListener mobKillListener = new MobKillListener(playerManager, configManager, levelUpRewardsConfig,
                langManager);
        Bukkit.getPluginManager().registerEvents(mobKillListener, plugin);

        LevelUpListener levelUpListener = new LevelUpListener(langManager);
        Bukkit.getPluginManager().registerEvents(levelUpListener, plugin);

        // ===== Listeners de explorador =====
        ExplorerProgressStorage explorerProgressStorage = services.get(ExplorerProgressStorage.class);
        ExplorerJobListener explorerJobListener = new ExplorerJobListener(jobManager, playerManager,
                explorerProgressStorage, jobRewardService);
        Bukkit.getPluginManager().registerEvents(explorerJobListener, plugin);
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
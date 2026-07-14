package com.sack.rpgroll.core;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.CommandManager;
import com.sack.rpgroll.command.commands.*;
import com.sack.rpgroll.config.ConfigManager;
import com.sack.rpgroll.database.DatabaseManager;
import com.sack.rpgroll.gameplay.listener.LevelUpListener;
import com.sack.rpgroll.gameplay.listener.MobKillListener;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.gameplay.levelup.PlayerLevelUpHandler;
import com.sack.rpgroll.gameplay.skill.SkillRegistry;
import com.sack.rpgroll.gameplay.trait.TraitRegistry;
import com.sack.rpgroll.gui.listener.GUIListener;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.listener.PlayerEventListener;
import org.bukkit.Bukkit;

public class Bootstrap {

    private final RPGRoll plugin;
    private final ServiceRegistry services;

    public Bootstrap(RPGRoll plugin) {
        this.plugin = plugin;
        this.services = new ServiceRegistry();
    }

    public void initialize() {

        printBanner();

        registerCoreServices();

        registerCommands();

        registerEventListeners();

        plugin.getLogger().info("==================================");
        plugin.getLogger().info("RPGRoll iniciado correctamente.");
        plugin.getLogger().info("==================================");

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

        plugin.getLogger().info("✔ Todos los servicios detenidos correctamente.");

    }

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

        // 4. SkillRegistry - Carga de habilidades
        SkillRegistry skillRegistry = new SkillRegistry(plugin, configManager.getYamlLoader());
        skillRegistry.load();
        services.register(SkillRegistry.class, skillRegistry);
        plugin.getLogger().info("✔ SkillRegistry registrado");

        // 5. TraitRegistry - Carga de traits
        TraitRegistry traitRegistry = new TraitRegistry(plugin, configManager.getYamlLoader());
        traitRegistry.load();
        services.register(TraitRegistry.class, traitRegistry);
        plugin.getLogger().info("✔ TraitRegistry registrado");

        // 6. LevelUpRewardsConfig - Carga de recompensas de level up
        LevelUpRewardsConfig levelUpRewardsConfig = new LevelUpRewardsConfig(plugin, configManager.getYamlLoader());
        levelUpRewardsConfig.load();
        services.register(LevelUpRewardsConfig.class, levelUpRewardsConfig);
        plugin.getLogger().info("✔ LevelUpRewardsConfig registrado");

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
        commandManager.register(new ReloadCommand(plugin));

        // Registrar el comando principal /rpg
        plugin.getCommand("rpg").setExecutor(commandManager);
        plugin.getCommand("rpg").setTabCompleter(commandManager);

        services.register(CommandManager.class, commandManager);

        plugin.getLogger().info("✔ Sistema de comandos inicializado.");
        plugin.getLogger().info("");

    }

    private void registerEventListeners() {

        plugin.getLogger().info("Registrando event listeners...");

        // Listeners de jugador
        PlayerManager playerManager = services.get(PlayerManager.class);
        PlayerEventListener playerListener = new PlayerEventListener(playerManager);
        Bukkit.getPluginManager().registerEvents(playerListener, plugin);

        // Listeners de GUI
        GUIListener guiListener = new GUIListener();
        Bukkit.getPluginManager().registerEvents(guiListener, plugin);

        // Listeners de gameplay (XP, skills, etc)
        ConfigManager configManager = services.get(ConfigManager.class);
        LevelUpRewardsConfig levelUpRewardsConfig = services.get(LevelUpRewardsConfig.class);
        MobKillListener mobKillListener = new MobKillListener(playerManager, configManager, levelUpRewardsConfig);
        Bukkit.getPluginManager().registerEvents(mobKillListener, plugin);

        // Listener de celebración de level up
        LevelUpListener levelUpListener = new LevelUpListener();
        Bukkit.getPluginManager().registerEvents(levelUpListener, plugin);

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
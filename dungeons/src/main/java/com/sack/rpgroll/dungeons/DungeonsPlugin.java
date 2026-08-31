package com.sack.rpgroll.dungeons;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.dungeons.command.DungeonAdminCommand;
import com.sack.rpgroll.dungeons.command.DungeonCommand;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.gui.ChatPromptManager;
import com.sack.rpgroll.dungeons.integration.DungeonsPlaceholders;
import com.sack.rpgroll.dungeons.listener.DungeonCombatListener;
import com.sack.rpgroll.dungeons.listener.DungeonMobDeathListener;
import com.sack.rpgroll.dungeons.listener.DungeonObjectiveListener;
import com.sack.rpgroll.dungeons.listener.DungeonPlayerSessionListener;
import com.sack.rpgroll.dungeons.listener.DungeonSessionTask;
import com.sack.rpgroll.dungeons.player.DungeonPlayerStateManager;
import com.sack.rpgroll.dungeons.ranking.DungeonRankingManager;
import com.sack.rpgroll.dungeons.registry.ActionRegistry;
import com.sack.rpgroll.dungeons.registry.BuiltinDungeonActions;
import com.sack.rpgroll.dungeons.structure.StructureImportService;
import com.sack.rpgroll.dungeons.structure.StructureLibrary;
import com.sack.rpgroll.dungeons.structure.StructurePasteEngine;
import com.sack.rpgroll.guilds.GuildsAPI;
import com.sack.rpgroll.guilds.team.TeamManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class DungeonsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("dungeons", "structures");
    private static final long TICK_INTERVAL = 20L;

    private DungeonManager dungeonManager;
    private TeamManager teamManager;
    private DungeonPlayerStateManager stateManager;
    private DungeonRankingManager rankingManager;
    private ActionRegistry actionRegistry;
    private DungeonEngine engine;
    private ChatPromptManager chatPromptManager;
    private StructureLibrary structureLibrary;
    private StructurePasteEngine structurePasteEngine;
    private StructureImportService structureImportService;
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

        dungeonManager = new DungeonManager(this);
        dungeonManager.initialize();

        structureLibrary = new StructureLibrary(this);
        structureLibrary.initialize();
        structurePasteEngine = new StructurePasteEngine(this);
        structureImportService = new StructureImportService(this, structureLibrary);

        teamManager = GuildsAPI.getTeamManager();
        stateManager = new DungeonPlayerStateManager(this);
        rankingManager = new DungeonRankingManager(this);

        actionRegistry = new ActionRegistry(this);
        BuiltinDungeonActions.registerAll(actionRegistry, this);

        engine = new DungeonEngine(this, dungeonManager, stateManager, rankingManager, actionRegistry, langManager);

        chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        getServer().getPluginManager().registerEvents(new DungeonMobDeathListener(engine), this);
        getServer().getPluginManager().registerEvents(new DungeonCombatListener(engine), this);
        getServer().getPluginManager().registerEvents(new DungeonObjectiveListener(engine), this);
        getServer().getPluginManager().registerEvents(
                new DungeonPlayerSessionListener(engine, teamManager, stateManager), this);

        getServer().getScheduler().runTaskTimer(this, new DungeonSessionTask(engine), TICK_INTERVAL, TICK_INTERVAL);

        registerCommand("dungeon", new DungeonCommand(dungeonManager, teamManager, engine, langManager));
        registerCommand("dungeonadmin", new DungeonAdminCommand(dungeonManager, engine, chatPromptManager, this,
                structureLibrary, structurePasteEngine, structureImportService));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Dungeons habilitado. " + dungeonManager.count() + " mazmorra(s) cargadas.");
    }

    @Override
    public void onDisable() {

        if (stateManager != null) {
            stateManager.saveAll();
        }
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

    private void registerPlaceholders() {

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        new DungeonsPlaceholders(this, dungeonManager, engine, stateManager).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrolldungeons_...%)");
    }

    // ============ API pública para otros addons ============

    public DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public DungeonPlayerStateManager getStateManager() {
        return stateManager;
    }

    public DungeonRankingManager getRankingManager() {
        return rankingManager;
    }

    public ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public DungeonEngine getEngine() {
        return engine;
    }

    public StructureLibrary getStructureLibrary() {
        return structureLibrary;
    }

    public StructurePasteEngine getStructurePasteEngine() {
        return structurePasteEngine;
    }

    public LangManager getLangManager() {
        return langManager;
    }

}

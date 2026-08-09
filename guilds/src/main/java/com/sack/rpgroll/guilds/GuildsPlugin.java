package com.sack.rpgroll.guilds;

import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;

import com.sack.rpgroll.guilds.buff.BuffCalculator;
import com.sack.rpgroll.guilds.buff.BuffCombatListener;
import com.sack.rpgroll.guilds.buff.BuffTickTask;
import com.sack.rpgroll.guilds.command.GuildAdminCommand;
import com.sack.rpgroll.guilds.command.GuildCommand;
import com.sack.rpgroll.guilds.command.TeamCommand;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.GuildCreationRequirements;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.achievement.GuildAchievementChecker;
import com.sack.rpgroll.guilds.guild.chat.GuildChatListener;
import com.sack.rpgroll.guilds.guild.diplomacy.GuildDiplomacyManager;
import com.sack.rpgroll.guilds.guild.event.GuildEventReminderTask;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestService;
import com.sack.rpgroll.guilds.guild.quest.GuildResourceGatherListener;
import com.sack.rpgroll.guilds.guild.ranking.GuildRankingManager;
import com.sack.rpgroll.guilds.guild.territory.GuildTerritoryProtectionListener;
import com.sack.rpgroll.guilds.integration.GuildsPlaceholders;
import com.sack.rpgroll.guilds.listener.PlayerQuitCleanupListener;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.chat.TeamChatListener;
import com.sack.rpgroll.guilds.team.hud.TeamHudTask;
import com.sack.rpgroll.guilds.team.matchmaking.TeamMatchmakingQueue;
import com.sack.rpgroll.guilds.team.ping.TeamPingManager;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GuildsPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("quests", "guilds");

    private TeamManager teamManager;
    private TeamMatchmakingQueue matchmakingQueue;
    private TeamPingManager pingManager;
    private TeamChatListener teamChatListener;

    private GuildManager guildManager;
    private GuildQuestManager questManager;
    private GuildQuestService questService;
    private GuildDiplomacyManager diplomacyManager;
    private GuildChatListener guildChatListener;
    private GuildRankingManager rankingManager;

    private BuffCalculator buffCalculator;
    private ChatPromptManager chatPromptManager;
    private GuildServices services;

    @Override
    public void onEnable() {

        GuildsAPI.init(this);

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        teamManager = new TeamManager();
        matchmakingQueue = new TeamMatchmakingQueue(teamManager);
        pingManager = new TeamPingManager(teamManager);
        teamChatListener = new TeamChatListener(teamManager);
        getServer().getPluginManager().registerEvents(teamChatListener, this);

        guildManager = new GuildManager(this);
        guildManager.initialize();

        questManager = new GuildQuestManager(this);
        questManager.initialize();
        questService = new GuildQuestService(guildManager, questManager);

        diplomacyManager = new GuildDiplomacyManager(this);
        guildChatListener = new GuildChatListener(guildManager, diplomacyManager);
        getServer().getPluginManager().registerEvents(guildChatListener, this);

        rankingManager = new GuildRankingManager(guildManager);
        buffCalculator = new BuffCalculator(teamManager, guildManager);

        chatPromptManager = new ChatPromptManager(this);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        services = new GuildServices(teamManager, matchmakingQueue, pingManager, teamChatListener, guildManager,
                questManager, questService, diplomacyManager, guildChatListener, rankingManager, buffCalculator,
                chatPromptManager);

        getServer().getPluginManager().registerEvents(
                new GuildResourceGatherListener(guildManager, questService), this);
        getServer().getPluginManager().registerEvents(new GuildTerritoryProtectionListener(guildManager), this);
        getServer().getPluginManager().registerEvents(new BuffCombatListener(buffCalculator), this);
        getServer().getPluginManager().registerEvents(
                new PlayerQuitCleanupListener(teamManager, teamChatListener, guildChatListener), this);

        getServer().getScheduler().runTaskTimer(this, new TeamHudTask(teamManager), 20L, 20L);
        getServer().getScheduler().runTaskTimer(this, pingManager::tick, 10L, 10L);
        getServer().getScheduler().runTaskTimer(this, new BuffTickTask(buffCalculator), 40L, 40L);
        getServer().getScheduler().runTaskTimer(this, new GuildEventReminderTask(guildManager), 200L, 200L);
        getServer().getScheduler().runTaskTimer(this, this::checkAchievements, 600L, 600L);

        GuildCreationRequirements requirements = GuildCreationRequirements.fromConfig(
                getConfig().getConfigurationSection("guild-creation"));

        registerCommand("team", new TeamCommand(teamManager, matchmakingQueue, pingManager, teamChatListener,
                chatPromptManager));
        registerCommand("guild", new GuildCommand(guildManager, services, requirements));
        registerCommand("guildadmin", new GuildAdminCommand(guildManager, questManager, services));

        registerPlaceholders();

        getLogger().info("✔ RPGRoll-Guilds habilitado. " + guildManager.count() + " guild(s) cargadas, "
                + questManager.count() + " quest(s) de guild.");
    }

    private void checkAchievements() {
        GuildAchievementChecker checker = new GuildAchievementChecker();
        guildManager.getAll().forEach(checker::check);
    }

    @Override
    public void onDisable() {
        if (guildManager != null) {
            guildManager.saveAll();
        }
    }

    private void registerCommand(String name, CommandExecutor executor) {

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

        new GuildsPlaceholders(this, teamManager, guildManager).register();
        getLogger().info("✔ Placeholders registrados en PlaceholderAPI (%rpgrollguilds_...%)");
    }

    // ============ API pública para otros addons (ej. RPGRoll-Dungeons) ============

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public TeamMatchmakingQueue getMatchmakingQueue() {
        return matchmakingQueue;
    }

    public GuildManager getGuildManager() {
        return guildManager;
    }

    public GuildQuestService getQuestService() {
        return questService;
    }

    public BuffCalculator getBuffCalculator() {
        return buffCalculator;
    }

    public GuildServices getServices() {
        return services;
    }

}

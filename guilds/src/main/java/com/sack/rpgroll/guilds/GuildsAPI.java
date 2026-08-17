package com.sack.rpgroll.guilds;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.matchmaking.TeamMatchmakingQueue;

/**
 * Punto de entrada público para otros addons (ej. RPGRoll-Dungeons, que
 * depende de esta API para el sistema de equipos) — mismo patrón que
 * {@link com.sack.rpgroll.api.RPGRollAPI} en :core.
 */
public final class GuildsAPI {

    private static GuildsPlugin instance;

    private GuildsAPI() {
    }

    static void init(GuildsPlugin plugin) {
        instance = plugin;
    }

    public static boolean isReady() {
        return instance != null;
    }

    public static TeamManager getTeamManager() {
        return instance.getTeamManager();
    }

    public static TeamMatchmakingQueue getMatchmakingQueue() {
        return instance.getMatchmakingQueue();
    }

    public static GuildManager getGuildManager() {
        return instance.getGuildManager();
    }

    public static LangManager getLangManager() {
        return instance.getLangManager();
    }

    public static void reloadLangManager() {
        instance.reloadLangManager();
    }

}

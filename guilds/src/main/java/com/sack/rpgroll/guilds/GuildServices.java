package com.sack.rpgroll.guilds;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.guilds.buff.BuffCalculator;
import com.sack.rpgroll.guilds.gui.ChatPromptManager;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.chat.GuildChatListener;
import com.sack.rpgroll.guilds.guild.diplomacy.GuildDiplomacyManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestService;
import com.sack.rpgroll.guilds.guild.ranking.GuildRankingManager;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.chat.TeamChatListener;
import com.sack.rpgroll.guilds.team.matchmaking.TeamMatchmakingQueue;
import com.sack.rpgroll.guilds.team.ping.TeamPingManager;

/** Agrupa las dependencias compartidas entre comandos y GUIs para no repetir constructores enormes. */
public record GuildServices(
        TeamManager teamManager,
        TeamMatchmakingQueue matchmakingQueue,
        TeamPingManager pingManager,
        TeamChatListener teamChatListener,
        GuildManager guildManager,
        GuildQuestManager questManager,
        GuildQuestService questService,
        GuildDiplomacyManager diplomacyManager,
        GuildChatListener guildChatListener,
        GuildRankingManager rankingManager,
        BuffCalculator buffCalculator,
        ChatPromptManager chatPromptManager,
        LangManager langManager) {
}

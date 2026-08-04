package com.sack.rpgroll.guilds.guild.quest;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/** Alimenta el progreso de quests GATHER_RESOURCE cuando un miembro de guild rompe el material objetivo. */
public class GuildResourceGatherListener implements Listener {

    private final GuildManager guildManager;
    private final GuildQuestService questService;

    public GuildResourceGatherListener(GuildManager guildManager, GuildQuestService questService) {
        this.guildManager = guildManager;
        this.questService = questService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Guild guild = guildManager.findByMember(event.getPlayer().getUniqueId()).orElse(null);

        if (guild == null) {
            return;
        }

        String material = event.getBlock().getType().name();
        questService.reportProgress(guild, GuildQuestType.GATHER_RESOURCE, material, 1, event.getPlayer().getUniqueId());
        guild.statistics().addResourcesGathered(1);
    }

}

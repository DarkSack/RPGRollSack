package com.sack.rpgroll.guilds.listener;

import com.sack.rpgroll.guilds.guild.chat.GuildChatListener;
import com.sack.rpgroll.guilds.team.TeamManager;
import com.sack.rpgroll.guilds.team.chat.TeamChatListener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Limpia estado transitorio al desconectarse: invitaciones/equipo pendiente
 * (los equipos son temporales por diseño) y el modo de chat activo. La
 * membresía de guild, en cambio, persiste — desconectarse no expulsa a
 * nadie de su guild.
 */
public class PlayerQuitCleanupListener implements Listener {

    private final TeamManager teamManager;
    private final TeamChatListener teamChatListener;
    private final GuildChatListener guildChatListener;

    public PlayerQuitCleanupListener(TeamManager teamManager, TeamChatListener teamChatListener,
            GuildChatListener guildChatListener) {
        this.teamManager = teamManager;
        this.teamChatListener = teamChatListener;
        this.guildChatListener = guildChatListener;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        var uuid = event.getPlayer().getUniqueId();
        teamManager.onQuit(uuid);
        teamChatListener.disable(uuid);
        guildChatListener.disable(uuid);
    }

}

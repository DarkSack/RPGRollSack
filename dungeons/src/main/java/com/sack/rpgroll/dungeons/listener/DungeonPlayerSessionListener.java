package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.player.DungeonPlayerStateManager;
import com.sack.rpgroll.guilds.team.TeamManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Al desconectarse: sale del grupo temporal, guarda/descarga su estado
 * de cooldowns y sale de la corrida; si era el último conectado, la
 * corrida se abandona (evita corridas "fantasma" que nunca terminan).
 */
public class DungeonPlayerSessionListener implements Listener {

    private final DungeonEngine engine;
    private final TeamManager teamManager;
    private final DungeonPlayerStateManager stateManager;

    public DungeonPlayerSessionListener(DungeonEngine engine, TeamManager teamManager,
            DungeonPlayerStateManager stateManager) {
        this.engine = engine;
        this.teamManager = teamManager;
        this.stateManager = stateManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        UUID playerId = event.getPlayer().getUniqueId();

        teamManager.onQuit(playerId);
        stateManager.unload(playerId);

        engine.findSessionByPlayer(playerId).ifPresent(session -> engine.getDungeonManager()
                .get(session.dungeonId())
                .ifPresent(definition -> engine.leave(event.getPlayer(), session, definition)));
    }

}

package com.sack.rpgroll.dungeons.listener;

import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.player.DungeonPlayerStateManager;
import com.sack.rpgroll.guilds.team.TeamManager;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Al desconectarse: sale del grupo temporal, guarda/descarga su estado
 * de cooldowns, y si era el último miembro conectado de una corrida
 * activa, la abandona (evita corridas "fantasma" que nunca terminan).
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

        engine.findSessionByPlayer(playerId).ifPresent(session -> {

            boolean anyoneLeftOnline = session.partyMemberIds().stream()
                    .anyMatch(memberId -> !memberId.equals(playerId) && Bukkit.getPlayer(memberId) != null);

            if (!anyoneLeftOnline) {
                DungeonDefinition definition = engine.getDungeonManager().get(session.dungeonId()).orElse(null);
                if (definition != null) {
                    engine.abandon(session, definition);
                }
            }
        });
    }

}

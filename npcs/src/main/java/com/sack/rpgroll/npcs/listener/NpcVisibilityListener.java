package com.sack.rpgroll.npcs.listener;

import com.sack.rpgroll.npcs.core.NpcManager;
import com.sack.rpgroll.npcs.core.NpcSpawnManager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NpcVisibilityListener implements Listener {

    private final NpcManager npcManager;
    private final NpcSpawnManager spawnManager;

    public NpcVisibilityListener(NpcManager npcManager, NpcSpawnManager spawnManager) {
        this.npcManager = npcManager;
        this.spawnManager = spawnManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        spawnManager.updateVisibility(event.getPlayer(), npcManager.getAll());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return; // solo revisar en cambios de bloque horizontal, no cada micro-movimiento
        }

        spawnManager.updateVisibility(event.getPlayer(), npcManager.getAll());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        spawnManager.updateVisibility(event.getPlayer(), npcManager.getAll());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        spawnManager.removeViewer(event.getPlayer());
    }

}
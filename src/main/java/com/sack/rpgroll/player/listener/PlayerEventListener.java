package com.sack.rpgroll.player.listener;

import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * PlayerEventListener sincroniza el sistema RPG con eventos de Minecraft.
 * 
 * Eventos manejados:
 * - PlayerJoinEvent: Cargar/crear jugador RPG
 * - PlayerQuitEvent: Descargar y guardar jugador RPG
 */
public class PlayerEventListener implements Listener {

    private final PlayerManager playerManager;

    public PlayerEventListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    /**
     * Se ejecuta cuando un jugador entra al servidor.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        // Cargar o crear jugador RPG
        playerManager.loadOrCreate(event.getPlayer());

    }

    /**
     * Se ejecuta cuando un jugador sale del servidor.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        // Descargar y guardar jugador RPG
        playerManager.unloadPlayer(event.getPlayer().getUniqueId());

    }

}

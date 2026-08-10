package com.sack.rpgroll.tab.listener;

import com.sack.rpgroll.tab.core.RefreshCoordinator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Los únicos disparadores "genéricos" que RPGRoll-TAB conoce por sí mismo
 * (join/quit/cambio de mundo) — cualquier otro trigger específico de un
 * addon (subir de nivel, entrar a una dungeon, cambiar de guild) le
 * corresponde a ESE addon llamar {@code TABAPI.get().player(p).refresh()}.
 */
public class PlayerLifecycleListener implements Listener {

    private final RefreshCoordinator refreshCoordinator;

    public PlayerLifecycleListener(RefreshCoordinator refreshCoordinator) {
        this.refreshCoordinator = refreshCoordinator;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refreshCoordinator.refreshAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        refreshCoordinator.clearPlayer(event.getPlayer());
        refreshCoordinator.refreshAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        refreshCoordinator.refreshPlayer(event.getPlayer());
    }

}

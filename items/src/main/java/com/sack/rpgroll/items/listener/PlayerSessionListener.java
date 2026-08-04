package com.sack.rpgroll.items.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Libera el estado en memoria (loadout conocido, stat sheet) al desconectarse. */
public class PlayerSessionListener implements Listener {

    private final ItemEquipTask equipTask;

    public PlayerSessionListener(ItemEquipTask equipTask) {
        this.equipTask = equipTask;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        equipTask.clear(event.getPlayer());
    }

}

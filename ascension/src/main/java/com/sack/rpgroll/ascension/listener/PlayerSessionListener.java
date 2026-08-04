package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.ascension.engine.AscensionEngine;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Carga estado y reaplica bonos de atributo al entrar; guarda al salir. */
public class PlayerSessionListener implements Listener {

    private final AscensionEngine engine;

    public PlayerSessionListener(AscensionEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        engine.getStateManager().getOrLoad(event.getPlayer());
        engine.reapplyBonuses(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        engine.getStateManager().unload(event.getPlayer().getUniqueId());
    }

}

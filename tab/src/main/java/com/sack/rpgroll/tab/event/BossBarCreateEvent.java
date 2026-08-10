package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando se muestra una bossbar a un jugador (perfil, contexto o {@code TABService} directo). */
public class BossBarCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String bossBarId;

    public BossBarCreateEvent(Player player, String bossBarId) {
        this.player = player;
        this.bossBarId = bossBarId;
    }

    public Player player() {
        return player;
    }

    public String bossBarId() {
        return bossBarId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

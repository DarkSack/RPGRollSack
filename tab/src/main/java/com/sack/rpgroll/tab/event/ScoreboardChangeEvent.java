package com.sack.rpgroll.tab.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando el scoreboard sidebar activo de un jugador cambia (por perfil, contexto u override de la API). */
public class ScoreboardChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String previousScoreboardId;
    private final String newScoreboardId;

    public ScoreboardChangeEvent(Player player, String previousScoreboardId, String newScoreboardId) {
        this.player = player;
        this.previousScoreboardId = previousScoreboardId;
        this.newScoreboardId = newScoreboardId;
    }

    public Player player() {
        return player;
    }

    public String previousScoreboardId() {
        return previousScoreboardId;
    }

    public String newScoreboardId() {
        return newScoreboardId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

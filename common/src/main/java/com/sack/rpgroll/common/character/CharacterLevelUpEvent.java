package com.sack.rpgroll.common.character;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Un personaje subió de nivel. Lo dispara el core de RPGRoll y vive en
 * RPGRoll-Lib para que un módulo lo escuche sin compilar contra el core; sin
 * el core instalado, simplemente nunca llega.
 */
public class CharacterLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int previousLevel;
    private final int newLevel;

    public CharacterLevelUpEvent(Player player, int previousLevel, int newLevel) {
        this.player = player;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public Player getPlayer() {
        return player;
    }

    public int getPreviousLevel() {
        return previousLevel;
    }

    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

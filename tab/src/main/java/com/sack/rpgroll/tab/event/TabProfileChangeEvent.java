package com.sack.rpgroll.tab.event;

import com.sack.rpgroll.tab.profile.TABProfile;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando el perfil activo de un jugador cambia (join, contexto distinto, {@code /tabadmin profile}, API). */
public class TabProfileChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final TABProfile previousProfile;
    private final TABProfile newProfile;

    public TabProfileChangeEvent(Player player, TABProfile previousProfile, TABProfile newProfile) {
        this.player = player;
        this.previousProfile = previousProfile;
        this.newProfile = newProfile;
    }

    public Player player() {
        return player;
    }

    public TABProfile previousProfile() {
        return previousProfile;
    }

    public TABProfile newProfile() {
        return newProfile;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}

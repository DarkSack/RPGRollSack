package com.sack.rpgroll.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Se dispara al completar la creación de personaje (raza + clase elegidas
 * y guardadas). Útil para addons que quieran otorgar un kit inicial, un
 * mensaje de bienvenida custom, o registrar al jugador en su propio sistema.
 */
public class CharacterCreatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String raceId;
    private final String classId;

    public CharacterCreatedEvent(Player player, String raceId, String classId) {
        this.player = player;
        this.raceId = raceId;
        this.classId = classId;
    }

    public Player getPlayer() {
        return player;
    }

    public String getRaceId() {
        return raceId;
    }

    public String getClassId() {
        return classId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
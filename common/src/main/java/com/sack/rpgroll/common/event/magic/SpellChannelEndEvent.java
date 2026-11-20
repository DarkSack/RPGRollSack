package com.sack.rpgroll.common.event.magic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Termina la canalización de un hechizo, haya salido bien o no. Lo lanza
 * RPGRoll-Magic; lo escuchan RPGRoll-Emotes y otros módulos para cortar lo que
 * empezaron con el {@link SpellChannelStartEvent}. No es cancelable.
 */
public class SpellChannelEndEvent extends Event {

    /** Por qué acabó la canalización. */
    public enum Reason {
        /** Se completó y el hechizo se lanza. */
        COMPLETED,
        /** El jugador se movió. */
        MOVED,
        /** El jugador recibió daño. */
        DAMAGED,
        /** Cualquier otra causa (desconexión, muerte, cambio de hechizo...). */
        OTHER
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String spellId;
    private final Reason reason;

    public SpellChannelEndEvent(Player player, String spellId, Reason reason) {
        this.player = player;
        this.spellId = spellId;
        this.reason = reason;
    }

    public Player getPlayer() {
        return player;
    }

    public String spellId() {
        return spellId;
    }

    public Reason reason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

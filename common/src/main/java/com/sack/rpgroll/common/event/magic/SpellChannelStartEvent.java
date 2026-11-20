package com.sack.rpgroll.common.event.magic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Un jugador empieza a canalizar un hechizo con tiempo de lanzamiento. Lo lanza
 * RPGRoll-Magic al comenzar la canalización; lo escuchan RPGRoll-Emotes y otros
 * módulos (por ejemplo, para animar al jugador mientras dura). Siempre se cierra
 * con un {@link SpellChannelEndEvent}. No es cancelable.
 */
public class SpellChannelStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String spellId;
    private final String schoolId;
    private final int castTimeTicks;

    public SpellChannelStartEvent(Player player, String spellId, String schoolId, int castTimeTicks) {
        this.player = player;
        this.spellId = spellId;
        this.schoolId = schoolId;
        this.castTimeTicks = castTimeTicks;
    }

    public Player getPlayer() {
        return player;
    }

    public String spellId() {
        return spellId;
    }

    public String schoolId() {
        return schoolId;
    }

    /** Duración prevista de la canalización, en ticks. */
    public int castTimeTicks() {
        return castTimeTicks;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

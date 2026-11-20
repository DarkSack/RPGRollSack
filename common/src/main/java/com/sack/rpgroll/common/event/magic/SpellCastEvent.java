package com.sack.rpgroll.common.event.magic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Set;

/**
 * Un jugador acaba de lanzar un hechizo. Lo lanza RPGRoll-Magic cuando el
 * hechizo se ejecuta de verdad (tras la canalización, si la tiene); lo escuchan
 * RPGRoll-Emotes y cualquier otro módulo que quiera reaccionar a la magia sin
 * depender de Magic. No es cancelable.
 */
public class SpellCastEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String spellId;
    private final String schoolId;
    private final Set<String> tags;

    public SpellCastEvent(Player player, String spellId, String schoolId, Set<String> tags) {
        this.player = player;
        this.spellId = spellId;
        this.schoolId = schoolId;
        this.tags = Set.copyOf(tags);
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

    /** Etiquetas del hechizo; copia inmutable. */
    public Set<String> tags() {
        return tags;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

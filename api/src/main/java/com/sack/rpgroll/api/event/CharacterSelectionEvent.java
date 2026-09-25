package com.sack.rpgroll.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de {@link PlayerSelectRaceEvent} y {@link PlayerSelectClassEvent}:
 * un jugador está a punto de elegir raza o clase por su cuenta. Un addon
 * puede impedirlo con {@link #deny(String)}, dando el motivo que verá el
 * jugador (por ejemplo, RPGRoll-Ascension con las razas y clases secretas).
 * <p>
 * No se lanza cuando un admin cambia la raza o la clase: eso siempre pasa.
 */
public abstract class CharacterSelectionEvent extends Event implements Cancellable {

    /** Desde dónde se está eligiendo. */
    public enum Source {
        /** El asistente de creación de personaje. */
        CHARACTER_CREATION,
        /** {@code /race <id>} o {@code /class <id>}. */
        COMMAND
    }

    private final Player player;
    private final String selectedId;
    private final Source source;
    private final List<String> denyReasons = new ArrayList<>();
    private boolean cancelled;

    protected CharacterSelectionEvent(Player player, String selectedId, Source source) {
        this.player = player;
        this.selectedId = selectedId;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    /** Id de la raza o clase elegida. */
    public String getSelectedId() {
        return selectedId;
    }

    public Source getSource() {
        return source;
    }

    /** Cancela la elección y añade un motivo para el jugador. */
    public void deny(String reason) {
        denyReasons.add(reason);
        cancelled = true;
    }

    public List<String> getDenyReasons() {
        return List.copyOf(denyReasons);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

}

package com.sack.rpgroll.quests.core;

import java.util.List;

/**
 * Conversación de un stage: líneas secuenciales de npc/jugador, y
 * opcionalmente una decisión al final (lista de {@link DialogOption}) que
 * determina a qué stage salta la quest.
 */
public record Dialog(List<DialogLine> lines, List<DialogOption> options) {

    public Dialog {
        lines = lines == null ? List.of() : List.copyOf(lines);
        options = options == null ? List.of() : List.copyOf(options);
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

}

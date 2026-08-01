package com.sack.rpgroll.npcs.core;

import java.util.List;
import java.util.Objects;

/**
 * Un ítem clickeable dentro de un NpcMenuDefinition. Al hacer click,
 * ejecuta su lista de NpcAction (mismo sistema que las acciones del NPC).
 */
public record NpcMenuItem(
        int slot,
        String material,
        String displayName,
        List<String> lore,
        List<NpcAction> actions) {

    public NpcMenuItem {
        Objects.requireNonNull(material, "material no puede ser null");
        displayName = displayName == null ? "" : displayName;
        lore = lore == null ? List.of() : List.copyOf(lore);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

}
package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.content.RPGContent;

/**
 * Representa un Trait (característica/rasgo especial) del juego.
 * Los traits dan bonificaciones pasivas al jugador.
 */
public record Trait(
        String id,
        String name,
        String description,
        int requiredLevel,
        TraitEffect effect) implements RPGContent {

    /**
     * Obtiene la información formateada del trait.
     */
    public String getFormattedInfo() {
        return name + " (Nivel " + requiredLevel + ")";
    }

}

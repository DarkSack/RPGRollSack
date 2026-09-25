package com.sack.rpgroll.ascension.deferred;

import com.sack.rpgroll.ascension.core.AscensionRequirements;
import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un título que el jugador puede llevar (ej. "El Elegido"). Se gana como
 * recompensa de un logro o de un rango de facción, a mano con
 * {@code /ascendadmin title grant}, o solo, cuando se cumplen sus
 * {@code requirements} si los tiene.
 *
 * @param requirements {@code null} si el título no se desbloquea solo
 */
public record Title(String id, String displayName, AscensionRequirements requirements) implements RPGContent {

    public Title {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }

    public Title(String id, String displayName) {
        this(id, displayName, null);
    }

    public Title withDisplayName(String value) {
        return new Title(id, value, requirements);
    }

    public boolean unlocksAutomatically() {
        return requirements != null;
    }

}

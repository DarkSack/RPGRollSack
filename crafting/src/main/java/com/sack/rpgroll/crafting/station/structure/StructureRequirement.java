package com.sack.rpgroll.crafting.station.structure;

import java.util.Objects;

/**
 * Un bloque requerido alrededor del bloque disparador de una {@code CustomStation},
 * como offset relativo a él. Si la estación no define ninguno, no exige
 * ninguna estructura (comportamiento actual: un único bloque disparador).
 *
 * @param dx       offset relativo en X respecto al bloque disparador
 * @param dy       offset relativo en Y respecto al bloque disparador
 * @param dz       offset relativo en Z respecto al bloque disparador
 * @param material nombre de {@link org.bukkit.Material} exigido en esa posición
 */
public record StructureRequirement(int dx, int dy, int dz, String material) {

    public StructureRequirement {
        Objects.requireNonNull(material, "material no puede ser null");
    }

}

package com.sack.rpgroll.magic.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Un catalizador — varita, báculo, libro, cristal, amuleto, orbe o reliquia.
 * Mecánicamente todos son lo mismo: un ítem que hace falta sostener para
 * poder lanzar el hechizo seleccionado, con 3 multiplicadores de bonus
 * opcionales — lo que los diferencia es puramente material/nombre/lore.
 * "Modificar elementos" (que un báculo cambie la escuela de un hechizo)
 * queda fuera de esta pasada.
 *
 * @param powerMultiplier multiplica el daño/curación de los hechizos lanzados con este catalizador (1.0 = sin bonus)
 * @param costMultiplier  multiplica el costo de maná (1.0 = sin bonus, menor a 1.0 = reduce costo)
 * @param rangeMultiplier multiplica distancias/radios/alcance de los componentes del hechizo (1.0 = sin bonus)
 */
public record SpellCatalyst(
        String id,
        String displayName,
        String material,
        String description,
        double powerMultiplier,
        double costMultiplier,
        double rangeMultiplier) implements RPGContent {

    public SpellCatalyst {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        material = material == null || material.isBlank() ? "BLAZE_ROD" : material;
        description = description == null ? "" : description;
        powerMultiplier = powerMultiplier <= 0 ? 1.0 : powerMultiplier;
        costMultiplier = costMultiplier <= 0 ? 1.0 : costMultiplier;
        rangeMultiplier = rangeMultiplier <= 0 ? 1.0 : rangeMultiplier;
    }

}

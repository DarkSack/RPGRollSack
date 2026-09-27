package com.sack.rpgroll.furniture.core;

import java.util.Set;

/**
 * Cómo se coloca un mueble.
 *
 * @param surfaces  dónde se puede apoyar
 * @param rotations en cuántas direcciones puede mirar: 4 (cada 90°), 8 (cada 45°) o 16
 * @param limit     cuántos de este mueble caben en un chunk (0 = solo el límite global)
 */
public record Placement(Set<Surface> surfaces, int rotations, int limit) {

    public Placement {
        surfaces = Set.copyOf(surfaces);
        rotations = rotations >= 16 ? 16 : rotations >= 8 ? 8 : 4;
        limit = Math.max(0, limit);
    }

    public boolean allows(Surface surface) {
        return surfaces.contains(surface);
    }

    /** El paso de giro en grados. */
    public float step() {
        return 360f / rotations;
    }

    /** {@code yaw} redondeado a la dirección permitida más cercana, entre 0 y 360. */
    public float snap(float yaw) {

        float step = step();
        float snapped = Math.round(yaw / step) * step;
        return ((snapped % 360f) + 360f) % 360f;
    }
}

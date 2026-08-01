package com.sack.rpgroll.api.race;

/**
 * Modificadores físicos que una raza aplica sobre el Player real (no sobre
 * PlayerStats). Se aplican vía AttributeModifier de Bukkit y deben
 * reaplicarse en cada login, ya que Bukkit no los persiste entre reinicios.
 *
 * @param scale                multiplicador de tamaño (1.0 = normal)
 * @param movementSpeedPercent modificador porcentual de velocidad (-0.15 = 15%
 *                             más lento)
 * @param extraHealth          corazones extra sumados al máximo de vida (en
 *                             puntos, 2 = 1 corazón)
 * @param knockbackResistance  resistencia a empuje, 0.0-1.0 (0.2 = 20% menos
 *                             knockback recibido)
 */
public record RacePhysicalModifiers(
        double scale,
        double movementSpeedPercent,
        double extraHealth,
        double knockbackResistance) {

    public static RacePhysicalModifiers none() {
        return new RacePhysicalModifiers(1.0, 0.0, 0.0, 0.0);
    }

    public boolean hasAnyModifier() {
        return scale != 1.0 || movementSpeedPercent != 0.0 || extraHealth != 0.0 || knockbackResistance != 0.0;
    }

}
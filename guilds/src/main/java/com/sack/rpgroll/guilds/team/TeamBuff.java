package com.sack.rpgroll.guilds.team;

/**
 * Buffs configurables de equipo (spec: "+10% XP, +5% daño, +20%
 * regeneración, +15% velocidad"). Son toggles por equipo — el porcentaje
 * es fijo por diseño, no ajustable por jugadores, para evitar tener que
 * balancear un rango arbitrario.
 */
public enum TeamBuff {

    XP_BOOST(0.10, "Experiencia"),
    DAMAGE_BOOST(0.05, "Daño"),
    REGEN_BOOST(0.20, "Regeneración"),
    SPEED_BOOST(0.15, "Velocidad");

    private final double percent;
    private final String displayName;

    TeamBuff(double percent, String displayName) {
        this.percent = percent;
        this.displayName = displayName;
    }

    public double percent() {
        return percent;
    }

    public String displayName() {
        return displayName;
    }

}

package com.sack.rpgroll.guilds.team;

import com.sack.rpgroll.common.lang.LangManager;

/**
 * Buffs configurables de equipo (spec: "+10% XP, +5% daño, +20%
 * regeneración, +15% velocidad"). Son toggles por equipo — el porcentaje
 * es fijo por diseño, no ajustable por jugadores, para evitar tener que
 * balancear un rango arbitrario.
 */
public enum TeamBuff {

    XP_BOOST(0.10),
    DAMAGE_BOOST(0.05),
    REGEN_BOOST(0.20),
    SPEED_BOOST(0.15);

    private final double percent;

    TeamBuff(double percent) {
        this.percent = percent;
    }

    public double percent() {
        return percent;
    }

    public String displayName(LangManager lang) {
        return lang.raw("team.buff." + name().toLowerCase(java.util.Locale.ROOT));
    }

}

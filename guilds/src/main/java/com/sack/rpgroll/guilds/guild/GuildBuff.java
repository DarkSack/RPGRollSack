package com.sack.rpgroll.guilds.guild;

/** Buffs de guild (spec: "XP/loot/profesiones/economía/daño/defensa"). Activables hasta el cupo del árbol BUFFS. */
public enum GuildBuff {

    XP_BOOST(0.10, "Experiencia"),
    LOOT_BOOST(0.10, "Botín"),
    PROFESSION_BOOST(0.10, "Profesiones"),
    ECONOMY_BOOST(0.05, "Economía"),
    DAMAGE_BOOST(0.05, "Daño"),
    DEFENSE_BOOST(0.05, "Defensa");

    private final double percent;
    private final String displayName;

    GuildBuff(double percent, String displayName) {
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

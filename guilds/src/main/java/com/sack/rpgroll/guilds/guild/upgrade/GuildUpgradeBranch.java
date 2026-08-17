package com.sack.rpgroll.guilds.guild.upgrade;

import com.sack.rpgroll.common.lang.LangManager;

/** Ramas del árbol de mejoras de guild (spec: "Banco/Economía/Miembros/Buffs/Territorio/Eventos"). */
public enum GuildUpgradeBranch {

    BANK,
    ECONOMY,
    MEMBERS,
    BUFFS,
    TERRITORY,
    EVENTS;

    public static final int MAX_LEVEL = 10;

    public String displayName(LangManager lang) {
        return lang.raw("guild.branch." + name().toLowerCase(java.util.Locale.ROOT) + ".name");
    }

    public String description(LangManager lang) {
        return lang.raw("guild.branch." + name().toLowerCase(java.util.Locale.ROOT) + ".description");
    }

    /** Costo en dinero para pasar de {@code currentLevel} a {@code currentLevel + 1}. */
    public double upgradeCost(int currentLevel) {
        return 1000 * Math.pow(currentLevel + 1, 1.5);
    }

}

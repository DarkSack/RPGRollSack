package com.sack.rpgroll.guilds.guild.upgrade;

/** Ramas del árbol de mejoras de guild (spec: "Banco/Economía/Miembros/Buffs/Territorio/Eventos"). */
public enum GuildUpgradeBranch {

    BANK("Banco", "Más espacio de almacenamiento compartido"),
    ECONOMY("Economía", "Mejores recompensas de oro en quests de guild"),
    MEMBERS("Miembros", "Más cupo de integrantes"),
    BUFFS("Buffs", "Más buffs de guild activos a la vez"),
    TERRITORY("Territorio", "Más regiones reclamables"),
    EVENTS("Eventos", "Más eventos concurrentes en el calendario");

    public static final int MAX_LEVEL = 10;

    private final String displayName;
    private final String description;

    GuildUpgradeBranch(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    /** Costo en dinero para pasar de {@code currentLevel} a {@code currentLevel + 1}. */
    public double upgradeCost(int currentLevel) {
        return 1000 * Math.pow(currentLevel + 1, 1.5);
    }

}

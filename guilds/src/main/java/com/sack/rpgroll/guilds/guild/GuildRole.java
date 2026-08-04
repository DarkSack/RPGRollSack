package com.sack.rpgroll.guilds.guild;

/**
 * Roles dentro de una {@link Guild}. A diferencia de {@link
 * com.sack.rpgroll.guilds.team.TeamRole} (temporal), estos permisos rigen
 * una organización permanente: banco, territorio, diplomacia, etc.
 */
public enum GuildRole {

    LEADER(true, true, true, true),
    OFFICER(true, true, true, false),
    MEMBER(false, false, false, false),
    RECRUIT(false, false, false, false);

    private final boolean canInvite;
    private final boolean canKick;
    private final boolean canManageBank;
    private final boolean canManageSettings;

    GuildRole(boolean canInvite, boolean canKick, boolean canManageBank, boolean canManageSettings) {
        this.canInvite = canInvite;
        this.canKick = canKick;
        this.canManageBank = canManageBank;
        this.canManageSettings = canManageSettings;
    }

    public boolean canInvite() {
        return canInvite;
    }

    public boolean canKick() {
        return canKick;
    }

    /** Retirar dinero/ítems del vault, o gestionar impuestos. Depositar siempre está permitido a cualquier miembro. */
    public boolean canManageBank() {
        return canManageBank;
    }

    /** Territorio, capital, árbol de mejoras, diplomacia, calendario, personalización. */
    public boolean canManageSettings() {
        return canManageSettings;
    }

    public boolean outranks(GuildRole other) {
        return ordinal() < other.ordinal();
    }

}

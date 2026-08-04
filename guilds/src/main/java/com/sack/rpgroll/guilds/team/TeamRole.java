package com.sack.rpgroll.guilds.team;

/**
 * Roles dentro de un {@link Team}, cada uno con permisos independientes.
 * El orden de las constantes determina jerarquía (ordinal menor = más poder)
 * — usado por operaciones que requieren "rango suficiente" (kick/promote).
 */
public enum TeamRole {

    LEADER(true, true, true, true, true),
    OFFICER(true, true, false, true, true),
    MEMBER(false, false, false, false, true),
    GUEST(false, false, false, false, false);

    private final boolean canInvite;
    private final boolean canKick;
    private final boolean canEditConfig;
    private final boolean canManageBuffs;
    private final boolean canStartActivity;

    TeamRole(boolean canInvite, boolean canKick, boolean canEditConfig, boolean canManageBuffs,
            boolean canStartActivity) {
        this.canInvite = canInvite;
        this.canKick = canKick;
        this.canEditConfig = canEditConfig;
        this.canManageBuffs = canManageBuffs;
        this.canStartActivity = canStartActivity;
    }

    public boolean canInvite() {
        return canInvite;
    }

    public boolean canKick() {
        return canKick;
    }

    public boolean canEditConfig() {
        return canEditConfig;
    }

    public boolean canManageBuffs() {
        return canManageBuffs;
    }

    public boolean canStartActivity() {
        return canStartActivity;
    }

    /** @return true si este rol tiene rango suficiente para actuar sobre {@code other} (estrictamente mayor). */
    public boolean outranks(TeamRole other) {
        return ordinal() < other.ordinal();
    }

}

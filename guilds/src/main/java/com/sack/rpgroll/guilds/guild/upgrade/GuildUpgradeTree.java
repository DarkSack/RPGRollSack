package com.sack.rpgroll.guilds.guild.upgrade;

import java.util.EnumMap;
import java.util.Map;

/** Progreso independiente por rama, cada una desbloqueando capacidad/beneficios propios. */
public class GuildUpgradeTree {

    private final Map<GuildUpgradeBranch, Integer> levels = new EnumMap<>(GuildUpgradeBranch.class);

    public int level(GuildUpgradeBranch branch) {
        return levels.getOrDefault(branch, 0);
    }

    public boolean upgrade(GuildUpgradeBranch branch) {

        int current = level(branch);

        if (current >= GuildUpgradeBranch.MAX_LEVEL) {
            return false;
        }

        levels.put(branch, current + 1);
        return true;
    }

    public void restore(GuildUpgradeBranch branch, int level) {
        levels.put(branch, Math.max(0, Math.min(GuildUpgradeBranch.MAX_LEVEL, level)));
    }

    public Map<GuildUpgradeBranch, Integer> all() {
        return Map.copyOf(levels);
    }

    // ============ Efectos derivados ============

    public int vaultSlots() {
        return Math.min(45, 9 + level(GuildUpgradeBranch.BANK) * 9);
    }

    public double economyBonusPercent() {
        return level(GuildUpgradeBranch.ECONOMY) * 0.05;
    }

    public int maxMembers() {
        return 10 + level(GuildUpgradeBranch.MEMBERS) * 5;
    }

    public int maxActiveBuffs() {
        return 1 + level(GuildUpgradeBranch.BUFFS);
    }

    public int maxTerritories() {
        return 1 + level(GuildUpgradeBranch.TERRITORY);
    }

    public int maxConcurrentEvents() {
        return 1 + level(GuildUpgradeBranch.EVENTS);
    }

}

package com.sack.rpgroll.gameplay.stats;

import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

/**
 * Gestiona la asignación de puntos de estadística durante la creación
 * o progresión de un personaje.
 * <p>
 * Como {@link RPGPlayer} y {@link PlayerStats} son inmutables, este allocator
 * mantiene internamente la instancia más reciente de RPGPlayer y la reemplaza
 * en cada asignación exitosa. El resultado final se obtiene con
 * {@link #getPlayer()}.
 */
public class StatPointAllocator {

    private static final int MAX_STAT_VALUE = PlayerStats.MAX_STAT;

    private RPGPlayer player;
    private int availablePoints;

    public StatPointAllocator(RPGPlayer player, int availablePoints) {
        this.player = player;
        this.availablePoints = availablePoints;
    }

    public boolean allocate(String statInput, int amount) {
        if (amount <= 0 || amount > availablePoints) {
            return false;
        }

        StatType stat = StatType.fromString(statInput);
        if (stat == null) {
            return false;
        }

        PlayerStats currentStats = player.getStats();
        int currentValue = currentStats.get(stat);

        if (currentValue + amount > MAX_STAT_VALUE) {
            return false;
        }

        PlayerStats updatedStats = currentStats.with(stat, currentValue + amount);
        player = player.updateStats(updatedStats);
        availablePoints -= amount;
        return true;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public boolean hasPoints() {
        return availablePoints > 0;
    }

    public boolean isComplete() {
        return availablePoints == 0;
    }

    public RPGPlayer getPlayer() {
        return player;
    }
}
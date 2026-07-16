package com.sack.rpgroll.gameplay.stats;

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

    /**
     * Asigna puntos a una estadística del jugador.
     * Si la asignación es válida, actualiza internamente el RPGPlayer
     * a una nueva instancia con el stat modificado.
     *
     * @param statInput nombre o alias del stat (ej. "str", "fuerza", "strength")
     * @param amount    cantidad de puntos a asignar (debe ser positivo)
     * @return {@code true} si la asignación fue exitosa, {@code false} si es
     *         inválida
     */
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

    /**
     * Obtiene los puntos disponibles restantes.
     */
    public int getAvailablePoints() {
        return availablePoints;
    }

    /**
     * Verifica si hay puntos disponibles.
     */
    public boolean hasPoints() {
        return availablePoints > 0;
    }

    /**
     * Verifica si se gastaron todos los puntos.
     */
    public boolean isComplete() {
        return availablePoints == 0;
    }

    /**
     * Obtiene la instancia actualizada de RPGPlayer, con todas las
     * asignaciones de puntos aplicadas hasta el momento.
     */
    public RPGPlayer getPlayer() {
        return player;
    }
}
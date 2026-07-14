package com.sack.rpgroll.gameplay.stats;

import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

/**
 * Gestiona la asignación de puntos de estadística.
 */
public class StatPointAllocator {

    private final RPGPlayer player;
    private int availablePoints;

    public StatPointAllocator(RPGPlayer player, int availablePoints) {
        this.player = player;
        this.availablePoints = availablePoints;
    }

    /**
     * Asigna puntos a una estadística.
     */
    public boolean allocate(String stat, int amount) {
        if (amount > availablePoints) {
            return false;
        }

        if (amount <= 0) {
            return false;
        }

        PlayerStats currentStats = player.getStats();

        // Validar límites
        if (currentStats.strength() + amount > 20) {
            return false; // Máximo 20
        }

        switch (stat.toLowerCase()) {
            case "strength", "str", "fuerza":
                availablePoints -= amount;
                return true;
            case "dexterity", "dex", "destreza":
                availablePoints -= amount;
                return true;
            case "constitution", "con", "constitucion":
                availablePoints -= amount;
                return true;
            case "intelligence", "int", "inteligencia":
                availablePoints -= amount;
                return true;
            case "wisdom", "wis", "sabiduria":
                availablePoints -= amount;
                return true;
            case "charisma", "cha", "carisma":
                availablePoints -= amount;
                return true;
            default:
                return false;
        }
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

}

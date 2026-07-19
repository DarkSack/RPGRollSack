package com.sack.rpgroll.gameplay.skill;

import com.sack.rpgroll.content.RPGContent;

/**
 * Representa una habilidad (Skill) del juego.
 * Las skills son acciones especiales que los jugadores pueden aprender y usar.
 */
public record Skill(
        String id,
        String name,
        String description,
        int requiredLevel,
        int manaCost,
        int cooldownSeconds,
        double damageMultiplier) implements RPGContent {

    /**
     * Obtiene la información formateada de la skill.
     */
    public String getFormattedInfo() {
        return name + " (Nivel " + requiredLevel + ")";
    }

    /**
     * Verifica si el jugador puede usar esta skill.
     */
    public boolean canUse(int playerLevel, int currentMana) {
        return playerLevel >= requiredLevel && currentMana >= manaCost;
    }

}

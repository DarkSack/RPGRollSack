package com.sack.rpgroll.gameplay.combat;

/**
 * Representa las estadísticas de combate derivadas del jugador.
 * Se calcula basándose en PlayerStats, level, skills y traits.
 */
public record CombatStats(
        int maxHealth,
        int currentHealth,
        int maxMana,
        int currentMana,
        double armorRating,
        double evasionChance,
        double criticalChance,
        double criticalMultiplier) {

    /**
     * Crea CombatStats vacíos (valores por defecto).
     */
    public static CombatStats empty() {
        return new CombatStats(100, 100, 100, 100, 5.0, 0.1, 0.05, 1.5);
    }

    /**
     * Crea CombatStats a partir de estadísticas base.
     */
    public static CombatStats create(
            int constitutionBonus,
            int intelligenceBonus,
            int dexterityBonus,
            int level) {

        int maxHealth = 100 + (constitutionBonus * 5) + (level * 2);
        int maxMana = 100 + (intelligenceBonus * 5) + (level * 2);
        double armorRating = 5.0 + (level * 0.5);
        double evasionChance = 0.1 + (dexterityBonus * 0.02);
        double criticalChance = 0.05 + (dexterityBonus * 0.01);

        return new CombatStats(
                maxHealth,
                maxHealth,
                maxMana,
                maxMana,
                armorRating,
                evasionChance,
                criticalChance,
                1.5);
    }

    /**
     * Obtiene una copia con salud actualizada.
     */
    public CombatStats withHealth(int health) {
        return new CombatStats(
                maxHealth,
                Math.min(health, maxHealth),
                maxMana,
                currentMana,
                armorRating,
                evasionChance,
                criticalChance,
                criticalMultiplier);
    }

    /**
     * Obtiene una copia con mana actualizado.
     */
    public CombatStats withMana(int mana) {
        return new CombatStats(
                maxHealth,
                currentHealth,
                maxMana,
                Math.min(mana, maxMana),
                armorRating,
                evasionChance,
                criticalChance,
                criticalMultiplier);
    }

}

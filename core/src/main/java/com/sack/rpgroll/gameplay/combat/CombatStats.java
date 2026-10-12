package com.sack.rpgroll.gameplay.combat;

/**
 * Representa las estadísticas de combate derivadas del jugador.
 * maxHealth/maxMana crecen exclusivamente vía {@link #growHealth(int)} y
 * {@link #growMana(int)} al subir de nivel (ver LevelUpRewardsConfig) — no
 * se recalculan desde cero en cada carga, para no perder el progreso
 * acumulado. armorRating/evasionChance/criticalChance sí se recalculan
 * libremente a partir de las estadísticas actuales, ya que no acumulan
 * historial propio.
 * <p>
 * bonusHealth/bonusMana es la parte de los máximos que viene de rasgos y de
 * Ascension (ver PassiveEffects). No se guarda: el repositorio la descuenta
 * y se vuelve a calcular al entrar, así que quitar un rasgo no deja nada atrás.
 */
public record CombatStats(
        int maxHealth,
        int currentHealth,
        int maxMana,
        int currentMana,
        double armorRating,
        double evasionChance,
        double criticalChance,
        double criticalMultiplier,
        int bonusHealth,
        int bonusMana) {

    /**
     * Crea CombatStats vacíos (valores por defecto).
     */
    public static CombatStats empty() {
        return new CombatStats(100, 100, 100, 100, 5.0, 0.1, 0.05, 1.5, 0, 0);
    }

    /**
     * Crea CombatStats iniciales a partir de los modificadores de atributo
     * base (ver {@link com.sack.rpgroll.player.stats.PlayerStats}). Pensado
     * para usarse una sola vez, al crear el personaje — el crecimiento por
     * nivel posterior se aplica con {@link #growHealth(int)}/{@link
     * #growMana(int)}, no recalculando esta fórmula de nuevo.
     */
    public static CombatStats create(
            int constitutionModifier,
            int intelligenceModifier,
            int dexterityModifier,
            int level) {

        int maxHealth = 100 + (constitutionModifier * 5);
        int maxMana = 100 + (intelligenceModifier * 5);
        double armorRating = 5.0 + (level * 0.5);
        double evasionChance = 0.1 + (dexterityModifier * 0.02);
        double criticalChance = 0.05 + (dexterityModifier * 0.01);

        return new CombatStats(
                maxHealth,
                maxHealth,
                maxMana,
                maxMana,
                armorRating,
                evasionChance,
                criticalChance,
                1.5, 0, 0);
    }

    /**
     * Reconstruye CombatStats desde valores persistidos de salud/maná
     * (que sí acumulan progreso entre niveles), recalculando solo
     * armorRating/evasionChance/criticalChance a partir de las
     * estadísticas actuales — usado al cargar un jugador desde la BD.
     */
    public static CombatStats of(
            int maxHealth,
            int currentHealth,
            int maxMana,
            int currentMana,
            int dexterityModifier,
            int level) {

        double armorRating = 5.0 + (level * 0.5);
        double evasionChance = 0.1 + (dexterityModifier * 0.02);
        double criticalChance = 0.05 + (dexterityModifier * 0.01);

        return new CombatStats(
                maxHealth,
                currentHealth,
                maxMana,
                currentMana,
                armorRating,
                evasionChance,
                criticalChance,
                1.5, 0, 0);
    }

    /** Recalcula armadura, evasión y crítico con la destreza y el nivel de ahora; vida y maná no cambian. */
    public CombatStats refresh(int dexterityModifier, int level) {
        CombatStats fresh = of(maxHealth, currentHealth, maxMana, currentMana, dexterityModifier, level);
        return new CombatStats(
                maxHealth,
                currentHealth,
                maxMana,
                currentMana,
                fresh.armorRating,
                fresh.evasionChance,
                fresh.criticalChance,
                fresh.criticalMultiplier,
                bonusHealth,
                bonusMana);
    }

    /**
     * Cambia el bono de los máximos por este. Lo actual no sube con él (se regenera), solo se
     * recorta si ya no cabe. {@code withBonus(0, 0)} es lo que se guarda.
     */
    public CombatStats withBonus(int health, int mana) {
        // Por mucho que reste un rasgo, queda 1 de vida y 0 de maná.
        health = Math.max(health, 1 - (maxHealth - bonusHealth));
        mana = Math.max(mana, -(maxMana - bonusMana));
        int newMaxHealth = maxHealth - bonusHealth + health;
        int newMaxMana = maxMana - bonusMana + mana;
        return new CombatStats(
                newMaxHealth,
                Math.min(currentHealth, newMaxHealth),
                newMaxMana,
                Math.min(currentMana, newMaxMana),
                armorRating,
                evasionChance,
                criticalChance,
                criticalMultiplier,
                health,
                mana);
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
                criticalMultiplier,
                bonusHealth,
                bonusMana);
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
                criticalMultiplier,
                bonusHealth,
                bonusMana);
    }

    /**
     * Aumenta la salud máxima en {@code amount} y cura la misma cantidad
     * de salud actual (recompensa típica de level up).
     */
    public CombatStats growHealth(int amount) {
        return new CombatStats(
                maxHealth + amount,
                currentHealth + amount,
                maxMana,
                currentMana,
                armorRating,
                evasionChance,
                criticalChance,
                criticalMultiplier,
                bonusHealth,
                bonusMana);
    }

    /**
     * Aumenta el maná máximo en {@code amount} y restaura la misma
     * cantidad de maná actual (recompensa típica de level up).
     */
    public CombatStats growMana(int amount) {
        return new CombatStats(
                maxHealth,
                currentHealth,
                maxMana + amount,
                currentMana + amount,
                armorRating,
                evasionChance,
                criticalChance,
                criticalMultiplier,
                bonusHealth,
                bonusMana);
    }

}

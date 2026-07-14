package com.sack.rpgroll.player.stats;

/**
 * Estadísticas base de un jugador RPG (estilo D&D).
 * 
 * Record inmutable que representa los 6 atributos principales:
 * - Strength (Fuerza): Daño cuerpo a cuerpo
 * - Dexterity (Destreza): Precisión y evasión
 * - Constitution (Constitución): Vida y resistencia
 * - Intelligence (Inteligencia): Daño mágico
 * - Wisdom (Sabiduría): Regeneración de maná
 * - Charisma (Carisma): Comercio y diálogo
 */
public record PlayerStats(
        int strength,
        int dexterity,
        int constitution,
        int intelligence,
        int wisdom,
        int charisma) {

    public static final int DEFAULT_STAT = 10;
    public static final int MIN_STAT = 1;
    public static final int MAX_STAT = 20;

    /**
     * Factory method para crear estadísticas por defecto.
     * Se utilizan cuando un jugador crea su personaje.
     */
    public static PlayerStats createDefault() {
        return new PlayerStats(
                DEFAULT_STAT,
                DEFAULT_STAT,
                DEFAULT_STAT,
                DEFAULT_STAT,
                DEFAULT_STAT,
                DEFAULT_STAT);
    }

    /**
     * Constructor con validación.
     */
    public PlayerStats {
        if (strength < MIN_STAT || strength > MAX_STAT) {
            throw new IllegalArgumentException("Strength debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
        if (dexterity < MIN_STAT || dexterity > MAX_STAT) {
            throw new IllegalArgumentException("Dexterity debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
        if (constitution < MIN_STAT || constitution > MAX_STAT) {
            throw new IllegalArgumentException("Constitution debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
        if (intelligence < MIN_STAT || intelligence > MAX_STAT) {
            throw new IllegalArgumentException("Intelligence debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
        if (wisdom < MIN_STAT || wisdom > MAX_STAT) {
            throw new IllegalArgumentException("Wisdom debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
        if (charisma < MIN_STAT || charisma > MAX_STAT) {
            throw new IllegalArgumentException("Charisma debe estar entre " + MIN_STAT + " y " + MAX_STAT);
        }
    }

    /**
     * Obtiene el modificador de un atributo.
     * En D&D, el modificador = (atributo - 10) / 2
     */
    public int getStrengthModifier() {
        return (strength - 10) / 2;
    }

    public int getDexterityModifier() {
        return (dexterity - 10) / 2;
    }

    public int getConstitutionModifier() {
        return (constitution - 10) / 2;
    }

    public int getIntelligenceModifier() {
        return (intelligence - 10) / 2;
    }

    public int getWisdomModifier() {
        return (wisdom - 10) / 2;
    }

    public int CharismaModifier() {
        return (charisma - 10) / 2;
    }

    /**
     * Obtiene la suma de todos los atributos.
     */
    public int getTotalStats() {
        return strength + dexterity + constitution + intelligence + wisdom + charisma;
    }

}

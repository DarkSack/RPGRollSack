package com.sack.rpgroll.player.stats;

import com.sack.rpgroll.gameplay.stats.StatType;

/**
 * Estadísticas base de un jugador RPG (estilo D&D).
 * Record inmutable que representa los 6 atributos principales.
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

    public static PlayerStats createDefault() {
        return new PlayerStats(
                DEFAULT_STAT, DEFAULT_STAT, DEFAULT_STAT,
                DEFAULT_STAT, DEFAULT_STAT, DEFAULT_STAT);
    }

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

    public int getCharismaModifier() {
        return (charisma - 10) / 2;
    }

    public int getTotalStats() {
        return strength + dexterity + constitution + intelligence + wisdom + charisma;
    }

    /**
     * Obtiene el valor actual de un stat específico.
     */
    public int get(StatType stat) {
        return switch (stat) {
            case STRENGTH -> strength;
            case DEXTERITY -> dexterity;
            case CONSTITUTION -> constitution;
            case INTELLIGENCE -> intelligence;
            case WISDOM -> wisdom;
            case CHARISMA -> charisma;
        };
    }

    /**
     * Devuelve una nueva instancia de PlayerStats con un stat modificado.
     */
    public PlayerStats with(StatType stat, int value) {
        return switch (stat) {
            case STRENGTH -> new PlayerStats(value, dexterity, constitution, intelligence, wisdom, charisma);
            case DEXTERITY -> new PlayerStats(strength, value, constitution, intelligence, wisdom, charisma);
            case CONSTITUTION -> new PlayerStats(strength, dexterity, value, intelligence, wisdom, charisma);
            case INTELLIGENCE -> new PlayerStats(strength, dexterity, constitution, value, wisdom, charisma);
            case WISDOM -> new PlayerStats(strength, dexterity, constitution, intelligence, value, charisma);
            case CHARISMA -> new PlayerStats(strength, dexterity, constitution, intelligence, wisdom, value);
        };
    }
}
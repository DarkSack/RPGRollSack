package com.sack.rpgroll.gameplay.trait;

/**
 * Representa los efectos/bonificadores de un Trait.
 */
public record TraitEffect(
        int strengthBonus,
        int dexterityBonus,
        int constitutionBonus,
        int intelligenceBonus,
        int wisdomBonus,
        int charismaBonus,
        int healthBonus,
        int manaBonus,
        double damageBonus,
        double defenseBonus) {

    /**
     * Trait sin efectos.
     */
    public static TraitEffect empty() {
        return new TraitEffect(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Verifica si el trait tiene algún efecto.
     */
    public boolean hasEffects() {
        return strengthBonus != 0 || dexterityBonus != 0 || constitutionBonus != 0 ||
                intelligenceBonus != 0 || wisdomBonus != 0 || charismaBonus != 0 ||
                healthBonus != 0 || manaBonus != 0 || damageBonus != 0 || defenseBonus != 0;
    }

}

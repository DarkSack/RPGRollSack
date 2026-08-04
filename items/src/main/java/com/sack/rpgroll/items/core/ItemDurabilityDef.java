package com.sack.rpgroll.items.core;

/**
 * Durabilidad propia del ítem — independiente de la durabilidad vanilla
 * (el ítem se crea sin daño vanilla y este contador vive en su PDC).
 */
public record ItemDurabilityDef(
        int maxDurability,
        boolean repairable,
        int degradePerUse,
        int autoRepairPerMinute) {

    public static ItemDurabilityDef disabled() {
        return new ItemDurabilityDef(0, false, 0, 0);
    }

    public boolean enabled() {
        return maxDurability > 0;
    }

}

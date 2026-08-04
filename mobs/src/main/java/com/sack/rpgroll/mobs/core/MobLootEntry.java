package com.sack.rpgroll.mobs.core;

/**
 * Una entrada de la loot table. {@code reference} es el id de ítem de
 * RPGRoll-Items, un {@link org.bukkit.Material} vanilla, un comando o un
 * id de quest, según {@code type}.
 */
public record MobLootEntry(
        LootType type,
        String reference,
        int amountMin,
        int amountMax,
        double chance,
        int requiredLevel,
        LootScope scope) {

    public MobLootEntry {
        amountMin = Math.max(1, amountMin);
        amountMax = Math.max(amountMin, amountMax);
        chance = chance <= 0 ? 100.0 : chance;
        scope = scope == null ? LootScope.SHARED : scope;
    }

}

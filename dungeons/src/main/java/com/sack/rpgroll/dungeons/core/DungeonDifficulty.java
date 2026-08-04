package com.sack.rpgroll.dungeons.core;

import java.util.List;

/** Un modo de dificultad (Normal, Hard, Elite, Mythic, Nightmare... o cualquier id custom). */
public record DungeonDifficulty(
        String id,
        String displayName,
        double healthMultiplier,
        double damageMultiplier,
        double lootMultiplier,
        List<DungeonModifierType> modifiers) {

    public DungeonDifficulty {
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        healthMultiplier = healthMultiplier <= 0 ? 1.0 : healthMultiplier;
        damageMultiplier = damageMultiplier <= 0 ? 1.0 : damageMultiplier;
        lootMultiplier = lootMultiplier <= 0 ? 1.0 : lootMultiplier;
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
    }

    public static DungeonDifficulty defaultNormal() {
        return new DungeonDifficulty("normal", "Normal", 1.0, 1.0, 1.0, List.of());
    }

    public boolean hasModifier(DungeonModifierType type) {
        return modifiers.contains(type);
    }

}

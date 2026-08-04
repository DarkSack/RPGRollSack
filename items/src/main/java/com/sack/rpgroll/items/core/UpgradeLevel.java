package com.sack.rpgroll.items.core;

import java.util.Map;

/**
 * Un nivel de mejora ("Espada +N"). {@code statBonus} se suma (no
 * reemplaza) sobre los stats base del ítem; los overrides de apariencia y
 * rareza son opcionales — null significa "sin cambio en ese nivel".
 */
public record UpgradeLevel(
        int level,
        Map<String, Double> statBonus,
        String displayNameOverride,
        String rarityOverride,
        double cost,
        String costMaterial,
        int costAmount) {

    public UpgradeLevel {
        statBonus = statBonus == null ? Map.of() : Map.copyOf(statBonus);
    }

}

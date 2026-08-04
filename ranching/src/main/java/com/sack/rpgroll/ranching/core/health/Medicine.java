package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * @param curesDiseaseIds   vacío = no cura ninguna enfermedad puntual (ej. una vitamina, que solo da bonos)
 * @param cureChance        0-1, probabilidad de curar instantáneamente una enfermedad activa de {@code curesDiseaseIds}
 * @param recoveryBoostTicks si no cura instantáneo, cuánto acorta la duración restante de la enfermedad activa
 */
public record Medicine(String id, String displayName, String icon, String description, MedicineType type,
        Set<String> curesDiseaseIds, double cureChance, long recoveryBoostTicks, double healthBonus,
        double happinessBonus) implements RPGContent {

    public Medicine {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "POTION" : icon;
        description = description == null ? "" : description;
        type = type == null ? MedicineType.VITAMIN : type;
        curesDiseaseIds = curesDiseaseIds == null ? Set.of() : Set.copyOf(curesDiseaseIds);
        cureChance = Math.max(0, Math.min(1, cureChance));
        recoveryBoostTicks = Math.max(0, recoveryBoostTicks);
        healthBonus = Math.max(0, healthBonus);
        happinessBonus = Math.max(0, happinessBonus);
    }

}

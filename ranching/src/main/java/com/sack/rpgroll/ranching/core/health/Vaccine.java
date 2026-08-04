package com.sack.rpgroll.ranching.core.health;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * @param riskReduction          0-1, multiplica hacia abajo la chance de contagio/aparición de las
 *                               enfermedades de {@code preventsDiseaseIds} mientras la inmunidad está activa
 * @param immunityDurationTicks  0 = inmunidad permanente
 */
public record Vaccine(String id, String displayName, String icon, String description,
        Set<String> preventsDiseaseIds, double riskReduction, long immunityDurationTicks) implements RPGContent {

    public Vaccine {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "POTION" : icon;
        description = description == null ? "" : description;
        preventsDiseaseIds = preventsDiseaseIds == null ? Set.of() : Set.copyOf(preventsDiseaseIds);
        riskReduction = Math.max(0, Math.min(1, riskReduction));
        immunityDurationTicks = Math.max(0, immunityDurationTicks);
    }

    public boolean isPermanent() {
        return immunityDurationTicks == 0;
    }

}

package com.sack.rpgroll.seasons.core;

import java.util.Objects;

/**
 * Una subdivisión de una {@link Season} (ej. Primavera Temprana/Media/
 * Tardía) — comparte el clima/efectos de vegetación de la estación
 * padre, solo puede pisar la temperatura base con {@code
 * temperatureOverride} si no es null.
 */
public record SubSeason(String id, String displayName, int durationAmount, DurationUnit durationUnit,
        Double temperatureOverride) {

    public SubSeason {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        durationAmount = Math.max(1, durationAmount);
        durationUnit = durationUnit == null ? DurationUnit.MINECRAFT_DAYS : durationUnit;
    }

    public long durationTicks() {
        return durationUnit.toTicks(durationAmount);
    }

}

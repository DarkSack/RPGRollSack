package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * Una caña de pescar — unifica mecánicamente lo que el diseño original
 * separaba en Caña/Carrete/Sedal/Anzuelo: todos son, en la práctica,
 * multiplicadores sobre la misma tirada de captura. Lo que los
 * diferenciaría (modelo, lore, nombre) sigue siendo libre por catalizador.
 *
 * @param castPower    multiplica el alcance de pesca (cosmético por ahora, reservado para una futura mecánica de distancia real)
 * @param reelSpeed    achica la zona de tensión requerida en el minijuego RPG (más difícil) o la agranda si es &gt;1 (más fácil) — ver FishingMinigameManager
 * @param precision    bono directo a la tirada de calidad de la captura
 * @param resistance   aumenta los intentos fallidos permitidos antes de que el pez escape en el minijuego RPG
 * @param luckBonus    multiplica el peso de tirada de las especies raras/épicas/legendarias
 * @param preferredCategories nombres de {@link FishCategory} que reciben un bono de peso extra con esta caña
 */
public record FishingRod(
        String id,
        String displayName,
        String material,
        String description,
        int durability,
        double castPower,
        double reelSpeed,
        double precision,
        double resistance,
        double luckBonus,
        Set<String> preferredCategories) implements RPGContent {

    public FishingRod {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        material = material == null || material.isBlank() ? "FISHING_ROD" : material;
        description = description == null ? "" : description;
        durability = Math.max(1, durability);
        castPower = castPower <= 0 ? 1.0 : castPower;
        reelSpeed = reelSpeed <= 0 ? 1.0 : reelSpeed;
        precision = Math.max(0, precision);
        resistance = resistance <= 0 ? 1.0 : resistance;
        luckBonus = luckBonus <= 0 ? 1.0 : luckBonus;
        preferredCategories = preferredCategories == null ? Set.of() : Set.copyOf(preferredCategories);
    }

    /** Caña por defecto para cualquier caña de pescar vanilla sin etiquetar — sin ningún bono. */
    public static FishingRod defaultRod() {
        return new FishingRod("default", "Caña vanilla", "FISHING_ROD", "", 64, 1.0, 1.0, 0, 1.0, 1.0, Set.of());
    }

}

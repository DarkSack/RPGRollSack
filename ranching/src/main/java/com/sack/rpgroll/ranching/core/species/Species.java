package com.sack.rpgroll.ranching.core.species;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Una especie ganadera — ata la simulación (genética/reproducción/
 * producción) a un {@code EntityType} vanilla real. Las razas (ver
 * {@code Breed}) siempre pertenecen a exactamente una especie.
 *
 * @param entityType             nombre de un {@link org.bukkit.entity.EntityType} vanilla (COW, CHICKEN, SHEEP...)
 * @param productTypes           claves libres de lo que produce (ej. "milk", "meat", "leather")
 * @param baseProduction         cantidad base por ciclo de producción, por clave de {@code productTypes}
 * @param babyStageDurationTicks cuánto dura la etapa BABY antes de pasar a JUVENILE
 * @param juvenileStageDurationTicks cuánto dura JUVENILE antes de pasar a ADULT
 * @param elderThresholdTicks    cuánto tiempo como ADULT antes de pasar a ELDER (estadísticas reducidas)
 * @param gestationDurationTicks 0 = esta especie no gesta (nace directo, sin embarazo)
 * @param dietTags               tags de {@code Feed} que satisfacen la dieta de esta especie — vacío = cualquiera
 */
public record Species(
        String id,
        String displayName,
        String icon,
        String description,
        String entityType,
        Set<String> productTypes,
        Map<String, Double> baseProduction,
        double baseWeightMin,
        double baseWeightMax,
        long babyStageDurationTicks,
        long juvenileStageDurationTicks,
        long elderThresholdTicks,
        long gestationDurationTicks,
        int minLitterSize,
        int maxLitterSize,
        double baseFertility,
        Set<String> dietTags) implements RPGContent {

    public Species {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "COW_SPAWN_EGG" : icon;
        description = description == null ? "" : description;
        entityType = entityType == null || entityType.isBlank() ? "COW" : entityType.toUpperCase(java.util.Locale.ROOT);
        productTypes = productTypes == null ? Set.of() : Set.copyOf(productTypes);
        baseProduction = baseProduction == null ? Map.of() : Map.copyOf(baseProduction);
        baseWeightMin = Math.max(0.1, baseWeightMin);
        baseWeightMax = Math.max(baseWeightMin, baseWeightMax);
        babyStageDurationTicks = Math.max(0, babyStageDurationTicks);
        juvenileStageDurationTicks = Math.max(0, juvenileStageDurationTicks);
        elderThresholdTicks = Math.max(0, elderThresholdTicks);
        gestationDurationTicks = Math.max(0, gestationDurationTicks);
        minLitterSize = Math.max(1, minLitterSize);
        maxLitterSize = Math.max(minLitterSize, maxLitterSize);
        baseFertility = Math.max(0, Math.min(1, baseFertility));
        dietTags = dietTags == null ? Set.of() : Set.copyOf(dietTags);
    }

    public boolean gestates() {
        return gestationDurationTicks > 0;
    }

}

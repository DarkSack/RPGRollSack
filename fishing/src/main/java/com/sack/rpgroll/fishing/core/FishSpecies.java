package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * Una especie de pez — identidad, clasificación, requisitos de captura
 * (agua/bioma/profundidad/clima/hora/temporada/carnada) y las 4 legendarias
 * condiciones extra ({@code legendary}/{@code requiredLevel}/{@code
 * requiresFullMoon}/{@code requiredBaitId}) que hacen a un pez único.
 * <p>
 * Todo campo de tipo {@code Set} vacío significa "sin restricción" — un
 * Set vacío en {@code waterTypes} permite cualquier agua, no ninguna.
 *
 * @param customModelData 0 = sin modelo custom, usa la textura vanilla de {@code icon}
 * @param minWeight/maxWeight kg — el peso real de la captura se sortea en ese rango
 * @param attractedByBaitTags tags de {@link Bait} que dan bono de peso — vacío = cualquier carnada sirve igual
 * @param requiredBaitId       solo relevante si {@code legendary} — exige esa carnada exacta, no solo un tag
 * @param catchEffectId        id de un efecto de SackEffects a reproducir en el jugador al capturarlo, o null
 * @param catchStatusEffectId  id de un efecto de RPGRoll-Effects a aplicar al jugador al capturarlo, o null (ej. una anguila eléctrica que aturde)
 */
public record FishSpecies(
        String id,
        String displayName,
        String icon,
        int customModelData,
        String description,
        FishCategory category,
        FishRarity rarity,
        Set<WaterType> waterTypes,
        Set<String> biomes,
        Set<DepthRequirement> depths,
        double minWeight,
        double maxWeight,
        double minLength,
        double maxLength,
        double basePrice,
        int baseExperience,
        FishBehaviorType behavior,
        Set<String> allowedSeasons,
        Set<WeatherType> allowedWeathers,
        Set<TimeRequirement> allowedTimes,
        Set<String> attractedByBaitTags,
        boolean legendary,
        int requiredLevel,
        boolean requiresFullMoon,
        String requiredBaitId,
        String catchEffectId,
        String catchStatusEffectId) implements RPGContent {

    public FishSpecies {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "COD" : icon;
        description = description == null ? "" : description;
        category = category == null ? FishCategory.FRESHWATER : category;
        rarity = rarity == null ? FishRarity.COMMON : rarity;
        waterTypes = waterTypes == null ? Set.of() : Set.copyOf(waterTypes);
        biomes = biomes == null ? Set.of() : Set.copyOf(biomes);
        depths = depths == null ? Set.of() : Set.copyOf(depths);
        minWeight = Math.max(0.01, minWeight);
        maxWeight = Math.max(minWeight, maxWeight);
        minLength = Math.max(0.01, minLength);
        maxLength = Math.max(minLength, maxLength);
        basePrice = Math.max(0, basePrice);
        baseExperience = Math.max(0, baseExperience);
        behavior = behavior == null ? FishBehaviorType.SLOW : behavior;
        allowedSeasons = allowedSeasons == null ? Set.of() : Set.copyOf(allowedSeasons);
        allowedWeathers = allowedWeathers == null ? Set.of() : Set.copyOf(allowedWeathers);
        allowedTimes = allowedTimes == null ? Set.of() : Set.copyOf(allowedTimes);
        attractedByBaitTags = attractedByBaitTags == null ? Set.of() : Set.copyOf(attractedByBaitTags);
        requiredLevel = Math.max(0, requiredLevel);
        requiredBaitId = (requiredBaitId == null || requiredBaitId.isBlank()) ? null : requiredBaitId;
        catchEffectId = (catchEffectId == null || catchEffectId.isBlank()) ? null : catchEffectId;
        catchStatusEffectId = (catchStatusEffectId == null || catchStatusEffectId.isBlank()) ? null
                : catchStatusEffectId;
    }

    public boolean hasRequiredBait() {
        return requiredBaitId != null;
    }

}

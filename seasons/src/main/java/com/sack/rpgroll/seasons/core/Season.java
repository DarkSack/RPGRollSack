package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Una estación — clima, duración, subestaciones, modificadores de
 * temperatura por bioma, efectos de vegetación, mobs/jefe de temporada, y
 * qué eventos mundiales puede disparar mientras está activa.
 *
 * @param durationAmount           duración total de la estación (ignorando subestaciones) en {@code durationUnit}
 * @param subSeasons                divisiones opcionales — si no está vacío, sus duraciones deberían sumar aprox. durationAmount
 * @param biomeTemperatureModifiers delta en °C por nombre de bioma (ej. "taiga": -10.0), sumado a la base de {@code biome-temperatures.yml}
 * @param mobModifiers              mobs de RPGRoll-Mobs con spawn extra durante esta estación
 * @param exclusiveBossId           id de mob de RPGRoll-Mobs para un encuentro raro exclusivo de esta estación, o null
 * @param worldEventIds             ids de WorldEvent elegibles para disparo automático diario durante esta estación
 * @param worldEventDailyChance     fracción 0.0-1.0 evaluada una vez por día de Minecraft por mundo
 */
public record Season(
        String id,
        String displayName,
        String icon,
        String color,
        String description,
        int durationAmount,
        DurationUnit durationUnit,
        ClimateProfile climate,
        List<SubSeason> subSeasons,
        Map<String, Double> biomeTemperatureModifiers,
        Set<VegetationEffectType> vegetationEffects,
        List<SeasonMobModifier> mobModifiers,
        String exclusiveBossId,
        List<String> worldEventIds,
        double worldEventDailyChance,
        Set<String> tags) implements RPGContent {

    public Season {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "SUNFLOWER" : icon;
        color = color == null || color.isBlank() ? "WHITE" : color;
        description = description == null ? "" : description;
        durationAmount = Math.max(1, durationAmount);
        durationUnit = durationUnit == null ? DurationUnit.MINECRAFT_DAYS : durationUnit;
        climate = climate == null ? ClimateProfile.temperate() : climate;
        subSeasons = subSeasons == null ? List.of() : List.copyOf(subSeasons);
        biomeTemperatureModifiers = biomeTemperatureModifiers == null ? Map.of() : Map.copyOf(biomeTemperatureModifiers);
        vegetationEffects = vegetationEffects == null ? Set.of() : Set.copyOf(vegetationEffects);
        mobModifiers = mobModifiers == null ? List.of() : List.copyOf(mobModifiers);
        exclusiveBossId = (exclusiveBossId == null || exclusiveBossId.isBlank()) ? null : exclusiveBossId;
        worldEventIds = worldEventIds == null ? List.of() : List.copyOf(worldEventIds);
        worldEventDailyChance = Math.max(0.0, Math.min(1.0, worldEventDailyChance));
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public long durationTicks() {
        return durationUnit.toTicks(durationAmount);
    }

    public boolean hasSubSeasons() {
        return !subSeasons.isEmpty();
    }

    public boolean hasExclusiveBoss() {
        return exclusiveBossId != null;
    }

}

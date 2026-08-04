package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * Un alimento — se compara contra {@code Species#dietTags} para saber si
 * satisface la dieta de un animal (vacío en cualquiera de los dos lados
 * significa "sirve para cualquiera").
 *
 * @param nutritionValue   cuánto sacia por uso (0-100)
 * @param healthBonus      sumado directo a la salud del animal al alimentarlo
 * @param happinessBonus   sumado directo a la felicidad
 * @param productionBonus  bono temporal a la próxima producción del animal
 */
public record Feed(String id, String displayName, String icon, String description, FeedQuality quality,
        Set<String> tags, double nutritionValue, double healthBonus, double happinessBonus, double productionBonus)
        implements RPGContent {

    public Feed {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        icon = icon == null || icon.isBlank() ? "WHEAT" : icon;
        description = description == null ? "" : description;
        quality = quality == null ? FeedQuality.COMMON : quality;
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        nutritionValue = Math.max(0, nutritionValue);
        healthBonus = Math.max(0, healthBonus);
        happinessBonus = Math.max(0, happinessBonus);
        productionBonus = Math.max(0, productionBonus);
    }

    public boolean satisfiesDiet(Set<String> dietTags) {
        return dietTags.isEmpty() || tags.isEmpty() || !java.util.Collections.disjoint(tags, dietTags);
    }

}

package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;
import java.util.Set;

/**
 * Se sostiene en la mano secundaria mientras se pesca — se consume 1 por
 * cada tirada de caña (cast), sin importar si morder algo o no.
 *
 * @param tags                  para bonos de peso contra {@code FishSpecies#attractedByBaitTags}
 * @param qualityBonus          sumado directo a la tirada de calidad de la captura
 * @param legendaryWeightMultiplier multiplica el peso de tirada de especies legendarias — la "Carnada Legendaria" del diseño original
 */
public record Bait(String id, String displayName, String material, String description, Set<String> tags,
        double qualityBonus, double legendaryWeightMultiplier) implements RPGContent {

    public Bait {
        Objects.requireNonNull(id, "id no puede ser null");
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        material = material == null || material.isBlank() ? "STRING" : material;
        description = description == null ? "" : description;
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        qualityBonus = Math.max(0, qualityBonus);
        legendaryWeightMultiplier = legendaryWeightMultiplier <= 0 ? 1.0 : legendaryWeightMultiplier;
    }

}

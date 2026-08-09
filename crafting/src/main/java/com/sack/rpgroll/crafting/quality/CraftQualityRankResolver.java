package com.sack.rpgroll.crafting.quality;

import com.sack.rpgroll.crafting.ingredient.QualityRankResolver;

import java.util.Locale;

/** Traduce el nombre de un {@link CraftQuality} a su ordinal (mayor = mejor) para el matcher de ingredientes. */
public class CraftQualityRankResolver implements QualityRankResolver {

    @Override
    public int rankOf(String qualityId) {

        if (qualityId == null || qualityId.isBlank()) {
            return 0;
        }

        try {
            return CraftQuality.valueOf(qualityId.trim().toUpperCase(Locale.ROOT)).ordinal();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

}

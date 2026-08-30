package com.sack.rpgroll.crafting.quality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftQualityRankResolverTest {

    private final CraftQualityRankResolver resolver = new CraftQualityRankResolver();

    @Test
    void rankOfReturnsZeroForNullOrBlank() {
        assertEquals(0, resolver.rankOf(null));
        assertEquals(0, resolver.rankOf("  "));
    }

    @Test
    void rankOfReturnsZeroForUnknownQuality() {
        assertEquals(0, resolver.rankOf("not-a-quality"));
    }

    @Test
    void rankOfMatchesOrdinalCaseInsensitively() {
        assertEquals(CraftQuality.ROUGH.ordinal(), resolver.rankOf("rough"));
        assertEquals(CraftQuality.LEGENDARY.ordinal(), resolver.rankOf("LEGENDARY"));
    }

    @Test
    void higherQualityHasHigherRank() {
        assertTrue(resolver.rankOf("legendary") > resolver.rankOf("standard"));
    }
}

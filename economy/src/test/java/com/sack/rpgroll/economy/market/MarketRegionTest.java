package com.sack.rpgroll.economy.market;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketRegionTest {

    private MarketProduct product(String id, String category) {
        return new MarketProduct(id, null, null, null, 10, 0, 0, 0, 0, 0, 0, category, null);
    }

    @Test
    void containsHandlesReversedMinMaxCoordinates() {
        MarketRegion region = new MarketRegion("mine", null, "world", 10, 10, 10, -10, -10, -10, Map.of(), Map.of());

        assertTrue(region.contains("world", 0, 0, 0));
        assertTrue(region.contains("WORLD", -10, -10, -10));
        assertTrue(region.contains("world", 10, 10, 10));
        assertFalse(region.contains("world", 11, 0, 0));
    }

    @Test
    void containsIsFalseForDifferentWorld() {
        MarketRegion region = new MarketRegion("mine", null, "world", 0, 0, 0, 10, 10, 10, Map.of(), Map.of());
        assertFalse(region.contains("other_world", 5, 5, 5));
    }

    @Test
    void modifierForPrefersProductSpecificOverCategory() {
        MarketRegion region = new MarketRegion("mine", null, "world", 0, 0, 0, 10, 10, 10,
                Map.of("mineral", 0.7), Map.of("IRON_INGOT", 0.3));

        assertEquals(0.3, region.modifierFor(product("IRON_INGOT", "mineral")));
    }

    @Test
    void modifierForFallsBackToCategoryWhenNoProductOverride() {
        MarketRegion region = new MarketRegion("mine", null, "world", 0, 0, 0, 10, 10, 10,
                Map.of("mineral", 0.7), Map.of());

        assertEquals(0.7, region.modifierFor(product("GOLD_INGOT", "mineral")));
    }

    @Test
    void modifierForDefaultsToOneWhenNoOverrideMatches() {
        MarketRegion region = new MarketRegion("mine", null, "world", 0, 0, 0, 10, 10, 10, Map.of(), Map.of());
        assertEquals(1.0, region.modifierFor(product("GOLD_INGOT", "mineral")));
    }

    @Test
    void displayNameFallsBackToIdWhenBlank() {
        MarketRegion region = new MarketRegion("mine", " ", "world", 0, 0, 0, 10, 10, 10, null, null);
        assertEquals("mine", region.displayName());
    }

}

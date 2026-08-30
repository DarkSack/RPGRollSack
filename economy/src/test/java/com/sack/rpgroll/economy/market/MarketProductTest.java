package com.sack.rpgroll.economy.market;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketProductTest {

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new MarketProduct(" ", "Iron", null, null, 10, 0, 0, 0, 0, 0, 0, null, null));
    }

    @Test
    void displayNameFallsBackToIdWhenBlank() {
        MarketProduct product = new MarketProduct("IRON_INGOT", "  ", null, null, 10, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals("IRON_INGOT", product.displayName());
    }

    @Test
    void basePriceDefaultsToOneWhenNonPositive() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 0, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals(1.0, product.basePrice());
    }

    @Test
    void minAndMaxPriceDeriveFromBasePriceWhenNonPositive() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 20, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals(2.0, product.minPrice());
        assertEquals(200.0, product.maxPrice());
    }

    @Test
    void weightsAndVolatilityAndRecoveryDefaultWhenNonPositive() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 20, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals(1.0, product.supplyWeight());
        assertEquals(1.0, product.demandWeight());
        assertEquals(0.1, product.volatility());
        assertEquals(0.02, product.recoveryRate());
    }

    @Test
    void categoryDefaultsToMiscWhenBlank() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 20, 0, 0, 0, 0, 0, 0, "  ", null);
        assertEquals("misc", product.category());
    }

    @Test
    void iconDefaultsToPaperWhenBlank() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, "  ", null, 20, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals("PAPER", product.icon());
    }

    @Test
    void blankCurrencyIdBecomesNull() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, "  ", 20, 0, 0, 0, 0, 0, 0, null, null);
        assertEquals(null, product.currencyId());
    }

    @Test
    void nullSeasonTagModifiersBecomesEmptyMap() {
        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 20, 0, 0, 0, 0, 0, 0, null, null);
        assertTrue(product.seasonTagModifiers().isEmpty());
    }

    @Test
    void seasonTagModifiersIsCopiedNotReferenced() {
        Map<String, Double> mutable = new java.util.HashMap<>();
        mutable.put("harvest", 0.5);

        MarketProduct product = new MarketProduct("IRON_INGOT", null, null, null, 20, 0, 0, 0, 0, 0, 0, null, mutable);
        mutable.put("winter", 2.0);

        assertEquals(1, product.seasonTagModifiers().size());
    }

}

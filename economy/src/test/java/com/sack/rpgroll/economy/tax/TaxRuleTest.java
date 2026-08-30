package com.sack.rpgroll.economy.tax;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxRuleTest {

    @Test
    void constructorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> new TaxRule(" ", "Sales Tax", TaxType.SALE, 10, List.of(), true));
    }

    @Test
    void typeDefaultsToSaleWhenNull() {
        TaxRule rule = new TaxRule("sale-tax", null, null, 10, null, true);
        assertEquals(TaxType.SALE, rule.type());
    }

    @ParameterizedTest
    @CsvSource({"-5,0", "0,0", "50,50", "100,100", "150,100"})
    void ratePercentIsClampedToZeroToHundred(double input, double expected) {
        TaxRule rule = new TaxRule("sale-tax", null, TaxType.SALE, input, null, true);
        assertEquals(expected, rule.ratePercent());
    }

    @Test
    void appliesToIsCopiedAsImmutableList() {
        List<String> mutable = new java.util.ArrayList<>(List.of("mineral"));
        TaxRule rule = new TaxRule("sale-tax", null, TaxType.SALE, 10, mutable, true);
        mutable.add("food");

        assertEquals(1, rule.appliesTo().size());
    }

    @Test
    void matchesEverythingWhenAppliesToIsEmpty() {
        TaxRule rule = new TaxRule("sale-tax", null, TaxType.SALE, 10, List.of(), true);
        assertTrue(rule.matches("mineral"));
        assertTrue(rule.matches("anything"));
    }

    @Test
    void matchesOnlyListedCategoriesWhenAppliesToIsNotEmpty() {
        TaxRule rule = new TaxRule("sale-tax", null, TaxType.SALE, 10, List.of("mineral", "food"), true);
        assertTrue(rule.matches("mineral"));
        assertFalse(rule.matches("luxury"));
    }

}

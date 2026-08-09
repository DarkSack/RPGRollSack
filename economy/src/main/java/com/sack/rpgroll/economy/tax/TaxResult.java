package com.sack.rpgroll.economy.tax;

import java.util.List;

/** Resultado de aplicar {@link TaxEngine#apply}: cuánto se retuvo y con qué reglas. */
public record TaxResult(double grossAmount, double netAmount, double taxAmount, List<TaxRule> appliedRules) {

    public static TaxResult none(double amount) {
        return new TaxResult(amount, amount, 0, List.of());
    }

}

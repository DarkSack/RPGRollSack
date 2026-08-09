package com.sack.rpgroll.economy.tax;

import com.sack.rpgroll.economy.ledger.TransactionLedger;
import com.sack.rpgroll.economy.ledger.TransactionType;

import java.util.List;
import java.util.UUID;

/**
 * Aplica las {@link TaxRule} configuradas sobre un movimiento económico. El
 * monto retenido es un "money sink" puro (ver filosofía de la
 * documentación): sale de circulación, no se deposita en ninguna cuenta —
 * queda solo como un registro {@code TAX} en el libro mayor para que el
 * admin pueda ver cuánto se recaudó.
 */
public class TaxEngine {

    private final TaxRuleManager taxRuleManager;
    private final TransactionLedger ledger;

    public TaxEngine(TaxRuleManager taxRuleManager, TransactionLedger ledger) {
        this.taxRuleManager = taxRuleManager;
        this.ledger = ledger;
    }

    public TaxResult apply(TaxType type, String categoryOrId, double grossAmount, UUID actorId, String currencyId) {

        if (grossAmount <= 0) {
            return TaxResult.none(grossAmount);
        }

        List<TaxRule> applicable = taxRuleManager.rulesFor(type).stream()
                .filter(rule -> rule.matches(categoryOrId))
                .toList();

        if (applicable.isEmpty()) {
            return TaxResult.none(grossAmount);
        }

        double totalRatePercent = applicable.stream().mapToDouble(TaxRule::ratePercent).sum();
        totalRatePercent = Math.min(100, totalRatePercent);

        double taxAmount = grossAmount * (totalRatePercent / 100.0);
        double netAmount = grossAmount - taxAmount;

        if (taxAmount > 0) {
            ledger.record(actorId, TransactionType.TAX, currencyId, -taxAmount, netAmount,
                    "Impuesto (" + type + ") sobre " + categoryOrId + ": " + totalRatePercent + "%");
        }

        return new TaxResult(grossAmount, netAmount, taxAmount, applicable);
    }

}

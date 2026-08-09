package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.economy.api.EconomyAPI;
import com.sack.rpgroll.economy.ledger.TransactionType;
import com.sack.rpgroll.economy.wallet.EconomyResult;

import java.util.UUID;

/** Puente blando hacia RPGRoll-Economy para cobrar el costo monetario de una receta. */
public final class EconomyBridge {

    private EconomyBridge() {
    }

    public static boolean isReady() {
        return EconomyAPI.isReady();
    }

    /** @return true si se cobró correctamente (o el costo era 0); false si falta saldo o Economy no está instalado. */
    public static boolean charge(UUID playerId, String currencyId, double amount, String description) {

        if (amount <= 0) {
            return true;
        }

        if (!EconomyAPI.isReady()) {
            return false;
        }

        String resolvedCurrency = currencyId != null && !currencyId.isBlank()
                ? currencyId
                : EconomyAPI.get().currencies().defaultCurrency().id();

        EconomyResult result = EconomyAPI.get().wallet().withdraw(playerId, resolvedCurrency, amount,
                TransactionType.MISC, description);

        return result == EconomyResult.SUCCESS;
    }

}

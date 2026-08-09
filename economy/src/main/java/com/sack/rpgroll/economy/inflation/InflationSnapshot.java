package com.sack.rpgroll.economy.inflation;

import java.util.Map;

/** Una foto de la masa monetaria total (wallets + cuentas bancarias) por moneda, en un instante dado. */
public record InflationSnapshot(long timestampMillis, Map<String, Double> totalSupplyByCurrency) {
}

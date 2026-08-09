package com.sack.rpgroll.economy.currency;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Una moneda del ecosistema económico. El servidor puede tener varias
 * (oro, plata, tokens de evento, créditos...) — cada una vive en su propio
 * saldo dentro del {@code Wallet} de un jugador, independiente del resto.
 */
public record Currency(
        String id,
        String displayName,
        String symbol,
        int decimals,
        String icon,
        String color,
        double minBalance,
        double maxBalance,
        String permission,
        double exchangeRateToBase,
        boolean isBase) implements RPGContent {

    public Currency {
        Objects.requireNonNull(id, "id no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        displayName = displayName == null || displayName.isBlank() ? id : displayName;
        symbol = symbol == null || symbol.isBlank() ? "$" : symbol;
        decimals = Math.max(0, decimals);
        icon = icon == null || icon.isBlank() ? "SUNFLOWER" : icon;
        color = color == null || color.isBlank() ? "GOLD" : color;
        maxBalance = maxBalance <= 0 ? Double.MAX_VALUE : maxBalance;
        exchangeRateToBase = exchangeRateToBase <= 0 ? 1.0 : exchangeRateToBase;
    }

    public String format(double amount) {
        return String.format("%,." + decimals + "f%s", amount, symbol);
    }

}

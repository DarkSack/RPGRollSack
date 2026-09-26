package com.sack.rpgroll.economy.auction;

/** Los precios que escriben los jugadores. */
public final class AuctionPrices {

    private AuctionPrices() {
    }

    /** Número escrito por el jugador; acepta "1.5k", "2m" y comas de miles. NaN si no se entiende. */
    public static double parse(String text) {

        String value = text.trim().toLowerCase(java.util.Locale.ROOT).replace(",", "").replace(" ", "");
        double multiplier = 1;

        if (value.endsWith("k")) {
            multiplier = 1_000;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 1_000_000;
            value = value.substring(0, value.length() - 1);
        }

        try {
            double number = Double.parseDouble(value) * multiplier;
            return Double.isFinite(number) ? number : Double.NaN;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

}

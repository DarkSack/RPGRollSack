package com.sack.rpgroll.economy.auction;

import java.util.Comparator;

/** Orden del buscador. */
public enum AuctionSort {

    NEWEST(Comparator.comparingLong(AuctionListing::createdAtMillis).reversed()),
    ENDING_SOON(Comparator.comparingLong(AuctionListing::expiresAtMillis)),
    PRICE_LOW(Comparator.comparingDouble(AuctionListing::price)),
    PRICE_HIGH(Comparator.comparingDouble(AuctionListing::price).reversed());

    private final Comparator<AuctionListing> comparator;

    AuctionSort(Comparator<AuctionListing> comparator) {
        this.comparator = comparator;
    }

    public Comparator<AuctionListing> comparator() {
        // Desempate estable: con el mismo precio, primero la más antigua.
        return comparator.thenComparingLong(AuctionListing::createdAtMillis);
    }

    public String langKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public AuctionSort next() {
        return values()[(ordinal() + 1) % values().length];
    }

}

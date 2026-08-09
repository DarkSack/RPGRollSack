package com.sack.rpgroll.economy.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Un ítem publicado en la Casa de Subastas — compatible con cualquier ItemStack, incluidos los de RPGRoll-Items. */
public class AuctionListing {

    private final UUID id;
    private final UUID sellerId;
    private final ItemStack item;
    private final double startPrice;
    /** -1 = sin opción de "comprar ya". */
    private final double buyNowPrice;
    private final String currencyId;
    private final long expiresAtMillis;

    private double currentBid;
    private UUID currentBidderId;
    private boolean settled;

    public AuctionListing(UUID id, UUID sellerId, ItemStack item, double startPrice, double buyNowPrice,
            String currencyId, long expiresAtMillis) {
        this.id = id;
        this.sellerId = sellerId;
        this.item = item;
        this.startPrice = startPrice;
        this.buyNowPrice = buyNowPrice;
        this.currencyId = currencyId;
        this.expiresAtMillis = expiresAtMillis;
        this.currentBid = startPrice;
    }

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public ItemStack item() {
        return item;
    }

    public double startPrice() {
        return startPrice;
    }

    public double buyNowPrice() {
        return buyNowPrice;
    }

    public boolean hasBuyNow() {
        return buyNowPrice > 0;
    }

    public String currencyId() {
        return currencyId;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMillis;
    }

    public double currentBid() {
        return currentBid;
    }

    public UUID currentBidderId() {
        return currentBidderId;
    }

    public boolean hasBidder() {
        return currentBidderId != null;
    }

    public void placeBid(UUID bidderId, double amount) {
        this.currentBid = amount;
        this.currentBidderId = bidderId;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

}

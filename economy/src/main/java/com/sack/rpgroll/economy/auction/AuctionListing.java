package com.sack.rpgroll.economy.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Un ítem publicado en la Casa de Subastas — compatible con cualquier
 * ItemStack, incluidos los de RPGRoll-Items. Es de precio fijo (se compra
 * al instante) o una subasta con pujas y, opcionalmente, compra inmediata.
 */
public class AuctionListing {

    private final UUID id;
    private final UUID sellerId;
    private final String sellerName;
    private final String itemName;
    private final double startPrice;
    /** -1 = sin opción de "comprar ya". */
    private final double buyNowPrice;
    private final boolean biddable;
    private final String currencyId;
    private final long createdAtMillis;

    private ItemStack item;
    private long expiresAtMillis;
    private double currentBid;
    private UUID currentBidderId;
    private String currentBidderName;
    private boolean settled;

    public AuctionListing(UUID id, UUID sellerId, String sellerName, ItemStack item, String itemName,
            double startPrice, double buyNowPrice, boolean biddable, String currencyId, long createdAtMillis,
            long expiresAtMillis) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.buyNowPrice = buyNowPrice;
        this.biddable = biddable;
        this.currencyId = currencyId;
        this.createdAtMillis = createdAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.currentBid = startPrice;
    }

    /** Una publicación de precio fijo: solo se puede comprar al precio pedido. */
    public static AuctionListing fixed(UUID sellerId, String sellerName, ItemStack item, String itemName,
            double price, String currencyId, long now, long duration) {
        return new AuctionListing(UUID.randomUUID(), sellerId, sellerName, item, itemName, price, price, false,
                currencyId, now, now + duration);
    }

    /** Una subasta: se puja desde {@code startPrice}; {@code buyNowPrice} &lt;= 0 la deja sin compra inmediata. */
    public static AuctionListing auction(UUID sellerId, String sellerName, ItemStack item, String itemName,
            double startPrice, double buyNowPrice, String currencyId, long now, long duration) {
        return new AuctionListing(UUID.randomUUID(), sellerId, sellerName, item, itemName, startPrice,
                buyNowPrice > 0 ? buyNowPrice : -1, true, currencyId, now, now + duration);
    }

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public String sellerName() {
        return sellerName;
    }

    public ItemStack item() {
        return item;
    }

    /** Tras una entrega parcial (inventario lleno), lo que queda por recoger. */
    public void item(ItemStack item) {
        this.item = item;
    }

    public String itemName() {
        return itemName;
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

    public boolean biddable() {
        return biddable;
    }

    /** Lo que se muestra como precio: la puja actual en una subasta, el precio fijo si no. */
    public double price() {
        return biddable ? currentBid : buyNowPrice;
    }

    public String currencyId() {
        return currencyId;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public long expiresAtMillis() {
        return expiresAtMillis;
    }

    public void extendTo(long expiresAtMillis) {
        this.expiresAtMillis = expiresAtMillis;
    }

    public boolean isExpired(long now) {
        return now >= expiresAtMillis;
    }

    public boolean isExpired() {
        return isExpired(System.currentTimeMillis());
    }

    public double currentBid() {
        return currentBid;
    }

    public UUID currentBidderId() {
        return currentBidderId;
    }

    public String currentBidderName() {
        return currentBidderName;
    }

    public boolean hasBidder() {
        return currentBidderId != null;
    }

    public void placeBid(UUID bidderId, String bidderName, double amount) {
        this.currentBid = amount;
        this.currentBidderId = bidderId;
        this.currentBidderName = bidderName;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    /** Quién recoge el ítem una vez cerrada: el ganador o, sin comprador, el vendedor. */
    public UUID recipient() {
        return hasBidder() ? currentBidderId : sellerId;
    }

}

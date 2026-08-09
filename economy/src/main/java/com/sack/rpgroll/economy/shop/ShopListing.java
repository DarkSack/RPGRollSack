package com.sack.rpgroll.economy.shop;

import org.bukkit.Material;

/** Un ítem en venta dentro de una {@link PlayerShop}. */
public class ShopListing {

    private final Material material;
    private String displayName;
    private double unitPrice;
    /** -1 = stock ilimitado. */
    private int stock;

    public ShopListing(Material material, String displayName, double unitPrice, int stock) {
        this.material = material;
        this.displayName = displayName;
        this.unitPrice = unitPrice;
        this.stock = stock;
    }

    public Material material() {
        return material;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double unitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int stock() {
        return stock;
    }

    public boolean isUnlimited() {
        return stock < 0;
    }

    public void reduceStock(int amount) {
        if (!isUnlimited()) {
            stock = Math.max(0, stock - amount);
        }
    }

}

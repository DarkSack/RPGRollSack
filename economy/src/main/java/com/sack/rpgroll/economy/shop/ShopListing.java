package com.sack.rpgroll.economy.shop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Un ítem en venta dentro de una {@link PlayerShop}. {@link #item()} es una
 * unidad del ítem tal cual (nombre, encantamientos, datos) y {@link #stock()}
 * cuántas unidades guarda de verdad la tienda: las que le entregó su dueño.
 * <p>
 * Antes solo se guardaba el material y el stock lo escribía el dueño a mano,
 * sin entregar nada: una tienda de jugador creaba ítems de la nada. El stock
 * ilimitado (-1) queda solo para las tiendas del servidor.
 */
public class ShopListing {

    private final ItemStack item;
    private String displayName;
    private double unitPrice;
    /** -1 = stock ilimitado (solo en tiendas del servidor). */
    private int stock;

    public ShopListing(Material material, String displayName, double unitPrice, int stock) {
        this(new ItemStack(material), displayName, unitPrice, stock);
    }

    public ShopListing(ItemStack item, String displayName, double unitPrice, int stock) {
        this.item = item.asOne();
        this.displayName = displayName;
        this.unitPrice = unitPrice;
        this.stock = stock;
    }

    /** Una unidad del ítem que se vende (copia: se puede modificar sin tocar la tienda). */
    public ItemStack item() {
        return item.clone();
    }

    public Material material() {
        return item.getType();
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

    public void addStock(int amount) {
        if (!isUnlimited()) {
            stock += amount;
        }
    }

    public void reduceStock(int amount) {
        if (!isUnlimited()) {
            stock = Math.max(0, stock - amount);
        }
    }

}

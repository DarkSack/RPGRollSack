package com.sack.rpgroll.common.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalDouble;

/**
 * Lo que vale UNA unidad de un ítem para quien compra de todo (el comprador de RPGRoll-Economy). Lo
 * pone el plugin que crea el ítem —el pez con su precio sorteado, el producto del rancho según su
 * calidad— y así nadie tiene que conocer los plugins de los demás.
 */
public final class SellValue {

    public static final NamespacedKey KEY = new NamespacedKey("rpgroll", "sell-value");

    private SellValue() {
    }

    /** Marca el valor por unidad; 0 o menos lo quita. */
    public static ItemStack set(ItemStack item, double valuePerUnit) {

        ItemMeta meta = item == null ? null : item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (valuePerUnit > 0) {
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.DOUBLE, valuePerUnit);
        } else {
            meta.getPersistentDataContainer().remove(KEY);
        }

        item.setItemMeta(meta);
        return item;
    }

    public static OptionalDouble get(ItemStack item) {

        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return OptionalDouble.empty();
        }

        Double value = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.DOUBLE);
        return value == null || value <= 0 ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

}

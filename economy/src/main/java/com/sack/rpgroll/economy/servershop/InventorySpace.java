package com.sack.rpgroll.economy.servershop;

import org.bukkit.inventory.ItemStack;

/**
 * Cuentas sobre el inventario del jugador, separadas de Bukkit para poder
 * probarlas: la tienda cobra solo si lo comprado cabe entero.
 */
public final class InventorySpace {

    private InventorySpace() {
    }

    /** Cuántas unidades de {@code template} caben en {@code storage} (huecos vacíos y pilas iguales). */
    public static int room(ItemStack[] storage, ItemStack template) {

        int max = template.getMaxStackSize();
        int room = 0;

        for (ItemStack slot : storage) {
            if (slot == null || slot.getType().isAir()) {
                room += max;
            } else if (slot.isSimilar(template)) {
                room += Math.max(0, max - slot.getAmount());
            }
        }

        return room;
    }

    /** Cuántas unidades iguales a {@code template} tiene (las que se podrían vender). */
    public static int count(ItemStack[] storage, ItemStack template) {

        int count = 0;

        for (ItemStack slot : storage) {
            if (slot != null && slot.isSimilar(template)) {
                count += slot.getAmount();
            }
        }

        return count;
    }

}

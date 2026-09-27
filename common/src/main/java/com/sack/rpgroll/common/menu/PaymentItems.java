package com.sack.rpgroll.common.menu;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Qué cuenta como "tener" o "pagar con" un material en los menús
 * (HAS_ITEM / TAKE_ITEM): el material de verdad, sin nombre, modelo,
 * encantamientos ni datos de plugin. Si no, una gema de RPGRoll-Items hecha
 * sobre EMERALD pasaba por esmeralda: servía para pagar y, peor, era lo que
 * se llevaba el cobro si estaba antes en la mochila. Lo usan también las
 * entregas de misiones.
 */
public final class PaymentItems {

    private PaymentItems() {
    }

    public static boolean isPlain(ItemStack item) {

        if (!item.hasItemMeta()) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        return !meta.hasDisplayName() && !meta.hasCustomModelData() && !meta.hasItemModel() && !meta.hasEnchants()
                && meta.getPersistentDataContainer().isEmpty();
    }

}

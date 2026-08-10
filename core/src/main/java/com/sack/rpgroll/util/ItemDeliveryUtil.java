package com.sack.rpgroll.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Helper de entrega de ítems reusado por todos los comandos "give"/"givekey" de RPGRoll y sus addons. */
public final class ItemDeliveryUtil {

    private ItemDeliveryUtil() {
    }

    /**
     * Agrega {@code item} al inventario de {@code player}; lo que no entra se tira al piso en su
     * ubicación en vez de perderse. Nunca falla silenciosamente: el sobrante siempre termina en
     * el inventario o en el suelo.
     *
     * @return true si todo el stack se entregó en el inventario (sin sobrante tirado al piso)
     */
    public static boolean deliver(Player player, ItemStack item) {

        var leftover = player.getInventory().addItem(item);

        if (leftover.isEmpty()) {
            return true;
        }

        leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        return false;
    }
}

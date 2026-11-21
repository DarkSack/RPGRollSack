package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.items.ItemsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * La única clase de la tienda que toca RPGRoll-Items: solo se carga si ese
 * plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class ItemsBridge {

    private ItemsBridge() {
    }

    static Optional<ItemStack> create(String itemId) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin items)) {
            return Optional.empty();
        }

        return items.getItemManager().get(itemId).map(definition -> items.getItemFactory().create(definition));
    }

    /**
     * Si {@code stack} es ese ítem de RPGRoll-Items sin mejorar. Por id y no por {@code isSimilar}: así cuenta
     * aunque el pack haya cambiado su nombre o su lore después de soltarse.
     */
    static boolean is(ItemStack stack, String itemId) {
        return id(stack).filter(itemId::equals).isPresent();
    }

    /** El id de RPGRoll-Items de {@code stack} si no está mejorado; vacío si no es de ese plugin o lo está. */
    static Optional<String> id(ItemStack stack) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin items)) {
            return Optional.empty();
        }

        var instances = items.getInstanceService();
        return instances.getUpgradeLevel(stack) == 0 ? instances.getId(stack) : Optional.empty();
    }

}

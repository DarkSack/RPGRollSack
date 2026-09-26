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

}

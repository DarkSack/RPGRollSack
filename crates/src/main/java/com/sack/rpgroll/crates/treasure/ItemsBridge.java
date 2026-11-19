package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.items.ItemsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * La única clase de los tesoros que toca RPGRoll-Items: solo se llama si ese plugin está activo, así que
 * sin él no hay NoClassDefFoundError.
 */
final class ItemsBridge {

    private ItemsBridge() {
    }

    static Optional<ItemStack> create(String itemId, int amount) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin items)) {
            return Optional.empty();
        }

        return items.getItemManager().get(itemId).map(definition -> {
            ItemStack item = items.getItemFactory().create(definition);
            item.setAmount(Math.min(amount, item.getMaxStackSize()));
            return item;
        });
    }

}

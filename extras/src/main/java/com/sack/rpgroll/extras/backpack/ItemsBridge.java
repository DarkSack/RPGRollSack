package com.sack.rpgroll.extras.backpack;

import com.sack.rpgroll.items.ItemsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.Optional;

/**
 * La única clase de las mochilas que toca RPGRoll-Items. Solo se llama tras
 * comprobar que ese plugin está activo, así que sin él no hay
 * NoClassDefFoundError.
 */
final class ItemsBridge {

    private ItemsBridge() {
    }

    static Optional<Material> material(String itemId) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin items)) {
            return Optional.empty();
        }

        return items.getItemManager().get(itemId).map(definition -> items.getItemFactory().create(definition).getType());
    }

}

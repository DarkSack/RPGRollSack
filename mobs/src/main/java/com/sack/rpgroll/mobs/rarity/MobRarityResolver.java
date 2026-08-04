package com.sack.rpgroll.mobs.rarity;

import com.sack.rpgroll.items.ItemsPlugin;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;

/**
 * Resuelve el color de un rarityId reutilizando el sistema de Rareza de
 * RPGRoll-Items si está presente (softdepend) — evita duplicar ese
 * sistema acá. Sin Items, cae a un color neutro.
 */
public class MobRarityResolver {

    public TextColor resolveColor(String rarityId) {

        if (isItemsAvailable()) {

            var plugin = (ItemsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Items");
            var rarity = plugin.getRarityManager().get(rarityId);

            if (rarity.isPresent()) {
                return rarity.get().color();
            }
        }

        return NamedTextColor.WHITE;
    }

    private boolean isItemsAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin;
    }

}

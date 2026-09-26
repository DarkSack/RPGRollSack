package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.enchantments.EnchantmentsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * La única clase de la tienda que toca RPGRoll-Enchantments: solo se carga si
 * ese plugin está activo, así que sin él no hay NoClassDefFoundError.
 */
final class EnchantmentsBridge {

    private EnchantmentsBridge() {
    }

    static Optional<ItemStack> book(String enchantmentId, int level) {

        if (!(Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments") instanceof EnchantmentsPlugin plugin)) {
            return Optional.empty();
        }

        return plugin.getEnchantmentManager().get(enchantmentId)
                .map(enchantment -> plugin.getEnchantmentItem().createBook(enchantment,
                        Math.min(level, enchantment.maxLevel())));
    }

}

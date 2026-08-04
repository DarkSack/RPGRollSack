package com.sack.rpgroll.items.integration;

import com.sack.rpgroll.enchantments.EnchantmentsPlugin;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.item.EnchantmentItem;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;

/**
 * Integración blanda con RPGRoll-Enchantments: aplica encantamientos
 * custom a un ítem recién creado si el addon está presente. Softdepend en
 * plugin.yml — todo acá se resuelve en runtime, nunca en tiempo de carga
 * de clases, para que RPGRoll-Items funcione perfectamente sin ese addon.
 */
public final class EnchantmentsIntegration {

    private EnchantmentsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments") instanceof EnchantmentsPlugin;
    }

    public static void applyCustomEnchantments(ItemStack item, Map<String, Integer> customEnchantments) {

        if (customEnchantments.isEmpty() || !isAvailable()) {
            return;
        }

        var plugin = (EnchantmentsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments");
        var manager = plugin.getEnchantmentManager();
        EnchantmentItem enchantmentItem = plugin.getEnchantmentItem();

        for (var entry : customEnchantments.entrySet()) {

            Optional<CustomEnchantment> enchantment = manager.get(entry.getKey().toLowerCase());

            if (enchantment.isPresent()) {
                enchantmentItem.apply(item, enchantment.get(), entry.getValue());
            }
        }
    }

}

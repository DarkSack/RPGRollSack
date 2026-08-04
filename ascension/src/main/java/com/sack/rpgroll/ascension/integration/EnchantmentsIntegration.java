package com.sack.rpgroll.ascension.integration;

import com.sack.rpgroll.enchantments.EnchantmentsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Integración blanda con RPGRoll-Enchantments: cuando un talento otorga un
 * encantamiento, se aplica al ítem que el jugador tiene en la mano en ese
 * momento. Softdepend — todo se resuelve en runtime.
 */
public final class EnchantmentsIntegration {

    private EnchantmentsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments") instanceof EnchantmentsPlugin;
    }

    public static void grantToHeldItem(Player player, String enchantId) {

        if (!isAvailable()) {
            return;
        }

        var plugin = (EnchantmentsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments");
        var manager = plugin.getEnchantmentManager();

        manager.get(enchantId.toLowerCase()).ifPresent(enchantment -> {

            ItemStack item = player.getInventory().getItemInMainHand();

            if (item != null && !item.getType().isAir()) {
                plugin.getEnchantmentItem().apply(item, enchantment, 1);
            }
        });
    }

}

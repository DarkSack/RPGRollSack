package com.sack.rpgroll.dungeons.integration;

import com.sack.rpgroll.items.ItemsPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Optional;

/**
 * Integración blanda con RPGRoll-Items: si el addon está presente, un id
 * de loot se resuelve primero contra sus definiciones de ítem; si no, se
 * interpreta como un {@link Material} vanilla directo.
 */
public final class ItemsIntegration {

    private ItemsIntegration() {
    }

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().getPlugin("RPGRoll-Items") instanceof ItemsPlugin;
    }

    public static boolean giveItem(Player player, String reference, int amount) {

        if (reference == null || reference.isBlank()) {
            return false;
        }

        ItemStack item = resolveFromItems(reference, amount).orElseGet(() -> resolveFromMaterial(reference, amount));

        if (item == null) {
            return false;
        }

        var leftover = player.getInventory().addItem(item);
        leftover.values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));

        return true;
    }

    private static Optional<ItemStack> resolveFromItems(String reference, int amount) {

        if (!isAvailable()) {
            return Optional.empty();
        }

        var plugin = (ItemsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Items");

        return plugin.getItemManager().get(reference).map(definition -> {
            ItemStack item = plugin.getItemFactory().create(definition);
            item.setAmount(Math.max(1, amount));
            return item;
        });
    }

    private static ItemStack resolveFromMaterial(String reference, int amount) {

        try {
            Material material = Material.valueOf(reference.trim().toUpperCase(Locale.ROOT));
            return new ItemStack(material, Math.max(1, amount));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}

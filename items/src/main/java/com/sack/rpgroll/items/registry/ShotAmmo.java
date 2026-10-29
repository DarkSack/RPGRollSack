package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * La munición de {@code SHOOT}: {@code ammo} es el id de un ítem de RPGRoll-Items (o
 * {@code minecraft:<material>} para uno vanilla) y {@code ammo-amount} cuántas gasta cada disparo.
 * En creativo no se gasta, pero hace falta llevarla igual.
 */
record ShotAmmo(String itemId, Material material, int amount) {

    static ShotAmmo from(ItemAction action) {
        String raw = action.param("ammo", "").trim();
        if (raw.isEmpty()) {
            return null;
        }
        int amount = Math.max(1, Integer.parseInt(action.param("ammo-amount", "1").trim()));
        if (raw.toLowerCase(Locale.ROOT).startsWith("minecraft:")) {
            Material material = Material.matchMaterial(raw);
            if (material == null) {
                throw new IllegalArgumentException("material de munición desconocido: " + raw);
            }
            return new ShotAmmo(null, material, amount);
        }
        return new ShotAmmo(raw, null, amount);
    }

    /** Quita la munición del inventario. Sin bastante, no quita nada y devuelve false. */
    boolean take(Player player, Plugin plugin) {
        NamespacedKey idKey = new NamespacedKey(plugin, "item-id");
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        int have = 0;
        for (ItemStack stack : contents) {
            if (matches(stack, idKey)) {
                have += stack.getAmount();
            }
        }
        if (have < amount) {
            return false;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        int left = amount;
        for (int slot = 0; slot < contents.length && left > 0; slot++) {
            ItemStack stack = contents[slot];
            if (!matches(stack, idKey)) {
                continue;
            }
            int used = Math.min(left, stack.getAmount());
            int remaining = stack.getAmount() - used;
            left -= used;
            stack.setAmount(remaining);
            inventory.setItem(slot, remaining > 0 ? stack : null);
        }
        return true;
    }

    private boolean matches(ItemStack stack, NamespacedKey idKey) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
        String id = meta != null ? meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING) : null;
        // Un material vanilla no cuenta los ítems de RPGRoll hechos sobre él (una célula sobre PAPER no es papel).
        return material != null ? stack.getType() == material && id == null : itemId.equalsIgnoreCase(id);
    }
}

package com.sack.rpgroll.enchantments.integration;

import com.sack.rpgroll.enchantments.item.EnchantmentItem;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;

/**
 * Expansión de PlaceholderAPI de Encantamientos: %rpgrollenchantments_&lt;placeholder&gt;%.
 * Todos los placeholders leen el ítem en la mano principal del jugador.
 */
public class EnchantmentsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final EnchantmentItem enchantmentItem;

    public EnchantmentsPlaceholders(Plugin plugin, EnchantmentItem enchantmentItem) {
        this.plugin = plugin;
        this.enchantmentItem = enchantmentItem;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollenchantments";
    }

    @Override
    public String getAuthor() {
        return "Sack";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {

        if (player == null) {
            return "";
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("helditem_count")) {
            return String.valueOf(enchantmentItem.getAll(held).size());
        }

        if (key.startsWith("helditem_has_")) {
            String id = key.substring("helditem_has_".length());
            return !id.isBlank() && enchantmentItem.getLevel(held, id) > 0 ? "si" : "no";
        }

        if (key.startsWith("helditem_") && key.endsWith("_level")) {
            String id = key.substring("helditem_".length(), key.length() - "_level".length());
            return id.isBlank() ? "" : String.valueOf(enchantmentItem.getLevel(held, id));
        }

        return "";
    }

}

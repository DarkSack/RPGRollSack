package com.sack.rpgroll.items.integration;

import com.sack.rpgroll.items.core.ItemDefinition;
import com.sack.rpgroll.items.core.ItemManager;
import com.sack.rpgroll.items.instance.ItemInstanceService;
import com.sack.rpgroll.items.stat.ItemStatEngine;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Optional;

/**
 * Expansión de PlaceholderAPI de Items: %rpgrollitems_&lt;placeholder&gt;%.
 * <code>stat_&lt;nombre&gt;</code> lee del motor de stats agregados del
 * jugador (armadura + mano principal/secundaria); <code>helditem_*</code>
 * lee solo el ítem en la mano principal.
 */
public class ItemsPlaceholders extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ItemManager itemManager;
    private final ItemInstanceService instanceService;
    private final ItemStatEngine statEngine;

    public ItemsPlaceholders(Plugin plugin, ItemManager itemManager, ItemInstanceService instanceService,
            ItemStatEngine statEngine) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.instanceService = instanceService;
        this.statEngine = statEngine;
    }

    @Override
    public String getIdentifier() {
        return "rpgrollitems";
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

        String key = params.toLowerCase(Locale.ROOT);

        if (key.startsWith("stat_")) {
            String statId = key.substring("stat_".length());
            return statId.isBlank() ? "" : formatNumber(statEngine.getStat(player, statId));
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<String> heldId = instanceService.getId(held);

        return switch (key) {
            case "helditem_id" -> heldId.orElse("-");
            case "helditem_name" -> heldId.flatMap(itemManager::get).map(ItemDefinition::displayName).orElse("-");
            case "helditem_rarity" -> heldId.flatMap(itemManager::get).map(ItemDefinition::rarityId).orElse("-");
            case "helditem_upgrade_level" -> String.valueOf(instanceService.getUpgradeLevel(held));
            case "helditem_durability" -> heldId.flatMap(itemManager::get)
                    .map(def -> String.valueOf(instanceService.getDurability(held, def.durability().maxDurability())))
                    .orElse("-");
            case "helditem_durability_max" -> heldId.flatMap(itemManager::get)
                    .map(def -> String.valueOf(def.durability().maxDurability()))
                    .orElse("-");
            default -> "";
        };
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

}

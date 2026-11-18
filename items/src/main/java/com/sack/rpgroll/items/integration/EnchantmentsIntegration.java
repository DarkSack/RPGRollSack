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
        return Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Enchantments")
                && Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments") instanceof EnchantmentsPlugin;
    }

    /** Tras rehacer el lore de un ítem: que RPGRoll-Enchantments vuelva a poner sus líneas. */
    public static void refreshLore(ItemStack item) {
        if (!isAvailable()) {
            return;
        }
        ((EnchantmentsPlugin) Bukkit.getPluginManager().getPlugin("RPGRoll-Enchantments")).getEnchantmentItem().refreshLore(item);
    }

    /**
     * Las armas de RPGRoll-Items (las que tienen daño o poder mágico) cuentan como WEAPON aunque su
     * material no sea un arma vanilla, como las varas y báculos sobre BREEZE_ROD.
     */
    public static void registerCategories(com.sack.rpgroll.items.instance.ItemInstanceService instances,
            com.sack.rpgroll.items.core.ItemManager items) {
        if (!isAvailable()) {
            return;
        }
        EnchantmentItem.addCategoryProvider(stack -> instances.getId(stack)
                .flatMap(items::get)
                .map(definition -> {
                    var stats = definition.stats();
                    boolean weapon = stats.getOrDefault("damage", 0.0) > 0 || stats.getOrDefault("magic_power", 0.0) > 0;
                    return weapon && !com.sack.rpgroll.enchantments.core.EnchantCategory.ARMOR.matches(stack.getType())
                            ? java.util.Set.of(com.sack.rpgroll.enchantments.core.EnchantCategory.WEAPON)
                            : java.util.Set.<com.sack.rpgroll.enchantments.core.EnchantCategory>of();
                })
                .orElse(java.util.Set.of()));
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

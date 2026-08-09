package com.sack.rpgroll.crafting.integration;

import com.sack.rpgroll.items.ItemsPlugin;
import com.sack.rpgroll.items.core.ItemDefinition;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Puente blando hacia RPGRoll-Items: no expone una clase {@code ItemsAPI}
 * pública propia, así que en vez de eso usamos directamente los getters
 * públicos de {@code ItemsPlugin} (mismo patrón que {@code resolveCoreInstance()}
 * usa para :core, solo que buscando el plugin por nombre en vez de asumirlo).
 * Permite que el resultado de una {@code CustomRecipe} sea un ítem
 * personalizado de RPGRoll-Items en vez de un material vanilla.
 */
public final class ItemsBridge {

    private ItemsBridge() {
    }

    public static boolean isReady() {
        return resolve() != null;
    }

    public static Optional<ItemStack> createItem(String itemDefinitionId) {

        ItemsPlugin plugin = resolve();
        if (plugin == null || itemDefinitionId == null) {
            return Optional.empty();
        }

        Optional<ItemDefinition> definition = plugin.getItemManager().get(itemDefinitionId);
        return definition.map(def -> plugin.getItemFactory().create(def));
    }

    private static ItemsPlugin resolve() {

        Plugin plugin = Bukkit.getPluginManager().getPlugin("RPGRoll-Items");
        return plugin instanceof ItemsPlugin itemsPlugin ? itemsPlugin : null;
    }

}

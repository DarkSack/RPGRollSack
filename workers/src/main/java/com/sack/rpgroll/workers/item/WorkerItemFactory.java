package com.sack.rpgroll.workers.item;

import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class WorkerItemFactory {

    private WorkerItemFactory() {
    }

    public static ItemStack createWarehouseDesignator() {

        ItemStack item = new ItemBuilder(Material.STICK)
                .setName(Component.text("Designador de Almacén", NamedTextColor.GOLD))
                .setLore(
                        Component.text("Click derecho en un cofre/baúl para", NamedTextColor.GRAY),
                        Component.text("marcarlo (o desmarcarlo) como almacén.", NamedTextColor.GRAY))
                .build();

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(WorkerItemKeys.WAREHOUSE_DESIGNATOR, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);

        return item;
    }

    public static boolean isWarehouseDesignator(ItemStack item) {

        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(WorkerItemKeys.WAREHOUSE_DESIGNATOR,
                PersistentDataType.BOOLEAN);
    }

}

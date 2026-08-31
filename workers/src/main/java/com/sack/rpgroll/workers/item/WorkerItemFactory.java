package com.sack.rpgroll.workers.item;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
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

    public static ItemStack createWarehouseDesignator(LangManager lang) {

        ItemStack item = new ItemBuilder(Material.STICK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("item.warehouse_designator.name"), NamedTextColor.GOLD))
                .setLore(
                        ComponentUtils.parseWithDefault(lang.raw("item.warehouse_designator.lore1"), NamedTextColor.GRAY),
                        ComponentUtils.parseWithDefault(lang.raw("item.warehouse_designator.lore2"), NamedTextColor.GRAY))
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

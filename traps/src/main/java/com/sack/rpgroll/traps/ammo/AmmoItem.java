package com.sack.rpgroll.traps.ammo;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/** El ítem físico de una munición, reconocible por su PersistentDataContainer. */
public final class AmmoItem {

    private static final String KEY = "ammo-id";

    private AmmoItem() {
    }

    public static NamespacedKey key(Plugin plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    public static ItemStack create(Plugin plugin, AmmoDefinition ammo, LangManager lang, int amount) {

        ItemStack item = new ItemStack(ammo.icon(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.displayName(ComponentUtils.parse(ammo.displayName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        if (!ammo.description().isBlank()) {
            lore.add(ComponentUtils.parse(ammo.description()).decoration(TextDecoration.ITALIC, false));
        }

        lore.add(ComponentUtils.parse(lang.raw("admin.ammo.item_lore", "amount", ammo.stackSize()))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);

        if (ammo.customModelData() != null) {
            meta.setCustomModelData(ammo.customModelData());
        }

        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.STRING, ammo.id());
        item.setItemMeta(meta);

        return item;
    }

    /** El id de munición del ítem, o null si no es munición del plugin. */
    public static String ammoIdOf(Plugin plugin, ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        return item.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
    }

}

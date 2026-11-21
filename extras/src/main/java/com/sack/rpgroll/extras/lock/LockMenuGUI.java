package com.sack.rpgroll.extras.lock;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Lo que el dueño puede hacer con su cerradura: quitarla o dejarla. */
final class LockMenuGUI extends InventoryGUI {

    private static final int REMOVE = 11;
    private static final int CANCEL = 15;

    private final LangManager lang;
    private final Runnable onRemove;

    LockMenuGUI(Player player, LangManager lang, Runnable onRemove) {
        super(player, lang.component("lock.menu_title"), 27);
        this.lang = lang;
        this.onRemove = onRemove;
    }

    private static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        var meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void build() {
        clear();
        setItem(REMOVE, named(Material.SHEARS, lang.component("lock.remove_button"),
                List.of(lang.component("lock.remove_lore"))));
        setItem(CANCEL, named(Material.ARROW, lang.component("lock.cancel"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getRawSlot() == REMOVE) {
            close();
            onRemove.run();
        } else if (event.getRawSlot() == CANCEL) {
            close();
        }
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}

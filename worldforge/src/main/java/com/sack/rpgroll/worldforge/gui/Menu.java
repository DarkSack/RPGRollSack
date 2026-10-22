package com.sack.rpgroll.worldforge.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * One open menu. Identified by being its own {@link InventoryHolder}, never by title or item
 * names. It belongs to one player and to one "generation" of the plugin's data: a click in a
 * menu opened before a reload is refused instead of acting on what that menu showed.
 */
public final class Menu implements InventoryHolder {

    private record Button(String permission, Consumer<Player> action) {
    }

    private final UUID owner;
    private final long generation;
    private final Inventory inventory;
    private final Map<Integer, Button> buttons = new HashMap<>();

    Menu(Player owner, long generation, String title, int rows) {
        this.owner = owner.getUniqueId();
        this.generation = generation;
        this.inventory = Bukkit.createInventory(this, rows * 9, Component.text(title, NamedTextColor.DARK_AQUA));
    }

    /** A clickable item; {@code permission} is checked again on every click. Null action = information only. */
    Menu item(int slot, Material material, String name, List<String> lore, String permission, Consumer<Player> action) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(l -> (Component) Component.text(l, l.startsWith("✘") ? NamedTextColor.RED
                : l.startsWith("⚠") ? NamedTextColor.GOLD : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)).toList());
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        inventory.setItem(slot, stack);
        if (action != null) {
            buttons.put(slot, new Button(permission, action));
        }
        return this;
    }

    UUID owner() {
        return owner;
    }

    long generation() {
        return generation;
    }

    /** The permission a click on this slot needs, or null if the slot does nothing. */
    String permission(int slot) {
        Button b = buttons.get(slot);
        return b == null ? null : b.permission();
    }

    Consumer<Player> action(int slot) {
        Button b = buttons.get(slot);
        return b == null ? null : b.action();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

package com.sack.rpgroll.sackresourcepack.gui;

import com.sack.rpgroll.sackresourcepack.gui.listener.GUIListener;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Base local para las GUIs de SackResourcePack — copia deliberada del
 * patrón usado en el resto del ecosistema RPGRoll (ver
 * {@code core/.../gui/InventoryGUI.java}), pero sin importar nada de
 * :core: este módulo es standalone y no debe depender de RPGRoll.
 */
public abstract class InventoryGUI {

    protected final Player player;
    protected final Inventory inventory;

    private boolean selectionMade = false;

    public InventoryGUI(Player player, Component title, int size) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, size, title);
    }

    public abstract void build();

    public abstract void handleClick(InventoryClickEvent event);

    /** Si es true, se reabre automáticamente al cerrar sin selección. */
    public boolean isSelectionRequired() {
        return false;
    }

    public boolean isSelectionMade() {
        return selectionMade;
    }

    protected void markSelectionMade() {
        this.selectionMade = true;
    }

    public void open() {
        build();
        GUIListener.registerGUI(player, this);
        player.openInventory(inventory);
    }

    public void close() {
        player.closeInventory();
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void clear() {
        inventory.clear();
    }

}

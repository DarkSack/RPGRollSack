package com.sack.rpgroll.gui;

import com.sack.rpgroll.gui.listener.GUIListener;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Clase base para todas las GUIs basadas en inventario.
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

    /**
     * Si es true, GUIListener reabrirá esta GUI automáticamente cuando
     * el jugador la cierre sin haber hecho una selección (ESC, click afuera).
     * Por defecto false — las GUIs deben declararse explícitamente obligatorias.
     */
    public boolean isSelectionRequired() {
        return false;
    }

    /**
     * @return true si el jugador ya completó una selección válida en esta GUI.
     */
    public boolean isSelectionMade() {
        return selectionMade;
    }

    /**
     * Marca que se realizó una selección válida. Debe llamarse en handleClick()
     * antes de close(), justo cuando se confirma la elección del jugador.
     */
    protected void markSelectionMade() {
        this.selectionMade = true;
    }

    public void open() {
        build();
        GUIListener.registerGUI(player, this);
        player.openInventory(inventory);
    }

    /**
     * Cierra el inventario. No desregistra la GUI aquí — eso lo hace
     * GUIListener al recibir InventoryCloseEvent, que es el único punto
     * que decide si corresponde reabrir por selección obligatoria.
     */
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
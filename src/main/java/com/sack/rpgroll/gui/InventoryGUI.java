package com.sack.rpgroll.gui;

import com.sack.rpgroll.gui.listener.GUIListener;
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

    public InventoryGUI(Player player, String title, int size) {
        this.player = player;
        this.inventory = Bukkit.createInventory(null, size, title);
    }

    /**
     * Construye y llena el inventario con items.
     */
    public abstract void build();

    /**
     * Maneja el click en el inventario.
     * 
     * @param event El evento de click
     */
    public abstract void handleClick(InventoryClickEvent event);

    /**
     * Abre el inventario para el jugador.
     */
    public void open() {
        build();
        GUIListener.registerGUI(player, this);
        player.openInventory(inventory);
    }

    /**
     * Cierra el inventario.
     */
    public void close() {
        GUIListener.unregisterGUI(player);
        player.closeInventory();
    }

    /**
     * Obtiene el inventario.
     */
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Obtiene el jugador.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Coloca un item en un slot específico.
     */
    protected void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    /**
     * Limpia el inventario.
     */
    protected void clear() {
        inventory.clear();
    }

}

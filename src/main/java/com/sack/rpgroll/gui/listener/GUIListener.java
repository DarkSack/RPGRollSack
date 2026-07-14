package com.sack.rpgroll.gui.listener;

import com.sack.rpgroll.gui.InventoryGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para manejar eventos de inventario de las GUIs.
 */
public class GUIListener implements Listener {

    // Mapa de inventarios activos (Player UUID -> GUI)
    private static final Map<UUID, InventoryGUI> activeGUIs = new HashMap<>();

    /**
     * Registra una GUI como activa para un jugador.
     */
    public static void registerGUI(Player player, InventoryGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }

    /**
     * Desregistra la GUI activa de un jugador.
     */
    public static void unregisterGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }

    /**
     * Obtiene la GUI activa de un jugador.
     */
    public static InventoryGUI getActiveGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryGUI gui = activeGUIs.get(player.getUniqueId());

        if (gui == null) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        // Verificar si el inventario clickeado es el de la GUI
        if (clickedInventory.equals(gui.getInventory())) {
            gui.handleClick(event);
        } else if (event.getView().getTopInventory().equals(gui.getInventory())) {
            // Prevenir clicks en el inventario del jugador mientras la GUI está abierta
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryGUI gui = activeGUIs.get(player.getUniqueId());

        if (gui != null && event.getInventory().equals(gui.getInventory())) {
            // Desregistrar GUI cuando se cierra
            unregisterGUI(player);
        }
    }

}

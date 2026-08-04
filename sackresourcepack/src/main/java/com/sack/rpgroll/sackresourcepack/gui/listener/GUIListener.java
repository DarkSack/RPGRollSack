package com.sack.rpgroll.sackresourcepack.gui.listener;

import com.sack.rpgroll.sackresourcepack.gui.InventoryGUI;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Despacha clicks/cierres de inventario a la GUI activa de cada jugador. */
public class GUIListener implements Listener {

    private static final Map<UUID, InventoryGUI> activeGUIs = new HashMap<>();

    private final Plugin plugin;

    public GUIListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void registerGUI(Player player, InventoryGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }

    public static void unregisterGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }

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

        if (clickedInventory.equals(gui.getInventory())) {
            gui.handleClick(event);
        } else if (event.getView().getTopInventory().equals(gui.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        InventoryGUI gui = activeGUIs.get(player.getUniqueId());

        if (gui == null || !event.getInventory().equals(gui.getInventory())) {
            return;
        }

        unregisterGUI(player);

        if (gui.isSelectionRequired() && !gui.isSelectionMade()) {

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    gui.open();
                }
            });
        }
    }

}

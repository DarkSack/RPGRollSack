package com.sack.rpgroll.economy.buyer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

/**
 * Las reglas de la ventana del comprador: arriba se deja y se quita libremente, la barra de abajo no
 * se toca (solo sus botones), y al cerrar —o al desconectarse— todo lo que no se vendió vuelve.
 */
public class BuyerListener implements Listener {

    private final Plugin plugin;

    public BuyerListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof BuyerMenu menu)) {
            return;
        }

        // Doble clic "recoger al cursor" podría sacar el relleno de la barra.
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        int raw = event.getRawSlot();

        if (raw >= BuyerMenu.DEPOSIT_SLOTS && raw < BuyerMenu.SIZE) {

            event.setCancelled(true);

            if (raw == BuyerMenu.SELL) {
                menu.sell();
            } else if (raw == BuyerMenu.CANCEL) {
                event.getWhoClicked().closeInventory();
            }
            return;
        }

        refreshLater(menu);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {

        if (!(event.getView().getTopInventory().getHolder() instanceof BuyerMenu menu)) {
            return;
        }

        for (int raw : event.getRawSlots()) {
            if (raw >= BuyerMenu.DEPOSIT_SLOTS && raw < BuyerMenu.SIZE) {
                event.setCancelled(true);
                return;
            }
        }

        refreshLater(menu);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof BuyerMenu menu) {
            menu.returnItems();
        }
    }

    /** Al apagar el plugin: se cierran las ventanas abiertas, así nadie pierde lo que dejó dentro. */
    public static void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BuyerMenu) {
                player.closeInventory();
            }
        }
    }

    private void refreshLater(BuyerMenu menu) {
        // El clic aún no se aplicó al inventario: se recalcula en el tick siguiente.
        Bukkit.getScheduler().runTask(plugin, menu::refresh);
    }

}

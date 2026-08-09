package com.sack.rpgroll.workers.listener;

import com.sack.rpgroll.workers.core.logistics.WarehouseManager;
import com.sack.rpgroll.workers.item.WorkerItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.block.Container;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Click derecho con el Designador de Almacén en un cofre/baúl — lo marca o desmarca como almacén. */
public class WarehouseDesignatorListener implements Listener {

    private final WarehouseManager warehouseManager;

    public WarehouseDesignatorListener(WarehouseManager warehouseManager) {
        this.warehouseManager = warehouseManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!WorkerItemFactory.isWarehouseDesignator(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }

        var block = event.getClickedBlock();

        if (block == null || !(block.getState() instanceof Container)) {
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);

        if (warehouseManager.remove(block.getLocation())) {
            event.getPlayer().sendMessage(Component.text("✔ Ya no es un almacén.", NamedTextColor.YELLOW));
            return;
        }

        warehouseManager.designate(block.getLocation(), "");
        event.getPlayer().sendMessage(Component.text("✔ Marcado como almacén — acepta cualquier recurso.",
                NamedTextColor.GREEN));
    }

}

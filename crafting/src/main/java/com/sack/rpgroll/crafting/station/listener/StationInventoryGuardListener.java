package com.sack.rpgroll.crafting.station.listener;

import com.sack.rpgroll.crafting.api.event.StationCloseEvent;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * El slot de salida de una estación es de solo-extracción: el jugador puede
 * sacar el resultado pero no puede meter ítems ahí a mano (eso rompería el
 * "cuánto hay disponible" que usa {@code StationProcessingEngine} para saber
 * si ya puede colocar el próximo resultado).
 */
public class StationInventoryGuardListener implements Listener {

    private final CustomStationManager stationManager;
    private final StationRuntimeRegistry runtimeRegistry;

    public StationInventoryGuardListener(CustomStationManager stationManager, StationRuntimeRegistry runtimeRegistry) {
        this.stationManager = stationManager;
        this.runtimeRegistry = runtimeRegistry;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {

        runtimeRegistry.findByInventory(event.getInventory()).ifPresent(runtime -> {

            stationManager.get(runtime.stationDefId()).ifPresent(station -> {

                boolean clickedOutputSlot = event.getRawSlot() == station.outputSlot();
                boolean isPlacingItem = event.getCursor() != null && !event.getCursor().getType().isAir();

                if (clickedOutputSlot && isPlacingItem) {
                    event.setCancelled(true);
                }
            });
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {

        runtimeRegistry.findByInventory(event.getInventory()).ifPresent(runtime -> {

            stationManager.get(runtime.stationDefId()).ifPresent(station -> {
                if (event.getRawSlots().contains(station.outputSlot())) {
                    event.setCancelled(true);
                }
            });
        });
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        runtimeRegistry.findByInventory(event.getInventory()).ifPresent(runtime ->
                stationManager.get(runtime.stationDefId()).ifPresent(station ->
                        Bukkit.getPluginManager().callEvent(new StationCloseEvent(player, station, runtime.key()))));
    }

}

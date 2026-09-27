package com.sack.rpgroll.furniture.listener;

import com.sack.rpgroll.furniture.FurnitureKeys;
import com.sack.rpgroll.furniture.placed.FurnitureIndex;
import com.sack.rpgroll.furniture.placed.PlacedFurniture;
import com.sack.rpgroll.furniture.seat.SeatService;
import com.sack.rpgroll.furniture.storage.StorageService;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.plugin.Plugin;

/**
 * Mantiene el índice al día con lo que se carga y descarga, y cierra bien asientos y almacenes.
 */
public class LifecycleListener implements Listener {

    private final Plugin plugin;
    private final FurnitureKeys keys;
    private final FurnitureIndex index;
    private final SeatService seats;
    private final StorageService storage;

    public LifecycleListener(Plugin plugin, FurnitureKeys keys, FurnitureIndex index, SeatService seats,
            StorageService storage) {
        this.plugin = plugin;
        this.keys = keys;
        this.index = index;
        this.seats = seats;
        this.storage = storage;
    }

    /** Al arrancar (o tras un reload): todo lo que ya estaba cargado. */
    public void indexLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                PlacedFurniture furniture = PlacedFurniture.of(display, keys);
                if (furniture != null) {
                    index.add(furniture);
                }
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {

        for (Entity entity : event.getEntities()) {
            PlacedFurniture furniture = PlacedFurniture.of(entity, keys);
            if (furniture != null) {
                index.add(furniture);
            } else if (seats.isSeat(entity)) {
                // Los asientos no se guardan; si alguno llegó al disco (un cierre brusco), sobra.
                entity.remove();
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {

        for (Entity entity : event.getEntities()) {
            PlacedFurniture furniture = PlacedFurniture.of(entity, keys);
            if (furniture != null) {
                storage.flush(furniture);
                seats.ejectAll(furniture);
                index.remove(furniture.uuid());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {

        if (event.getEntity() instanceof Player player && seats.isSeat(event.getDismounted())) {
            Entity stand = event.getDismounted();
            // Un tick después: durante el evento el jugador sigue montado y su posición es la del soporte.
            Bukkit.getScheduler().runTask(plugin, () -> seats.onDismount(player, stand));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Entity vehicle = event.getPlayer().getVehicle();
        if (vehicle != null && seats.isSeat(vehicle)) {
            vehicle.eject();
            vehicle.remove();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        if (StorageService.isStorage(event.getInventory())) {
            storage.onClose(event.getInventory(), event.getPlayer());
        } else if (StorageService.isTrash(event.getInventory())) {
            event.getInventory().clear();
        }
    }
}

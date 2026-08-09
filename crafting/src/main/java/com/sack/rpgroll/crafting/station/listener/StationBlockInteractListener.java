package com.sack.rpgroll.crafting.station.listener;

import com.sack.rpgroll.crafting.api.event.StationOpenEvent;
import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.crafting.station.runtime.StationRuntime;
import com.sack.rpgroll.crafting.station.runtime.StationRuntimeRegistry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Locale;
import java.util.Optional;

/**
 * Click derecho sobre un bloque cuyo material coincide con
 * {@code CustomStation#triggerBlockMaterial} abre el inventario propio de
 * esa estación (una por ubicación, creada la primera vez que se interactúa
 * con ese bloque específico).
 */
public class StationBlockInteractListener implements Listener {

    private final CustomStationManager stationManager;
    private final StationRuntimeRegistry runtimeRegistry;

    public StationBlockInteractListener(CustomStationManager stationManager, StationRuntimeRegistry runtimeRegistry) {
        this.stationManager = stationManager;
        this.runtimeRegistry = runtimeRegistry;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Block block = event.getClickedBlock();
        Material blockType = block.getType();

        Optional<CustomStation> matching = stationManager.getAll().stream()
                .filter(station -> matchesMaterial(station.triggerBlockMaterial(), blockType))
                .findFirst();

        if (matching.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        CustomStation station = matching.get();

        StationRuntime runtime = runtimeRegistry.getOrCreate(station.id(), block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), station.inventorySize(), station.guiTitle());

        StationOpenEvent openEvent = new StationOpenEvent(player, station, runtime.key());
        Bukkit.getPluginManager().callEvent(openEvent);
        if (openEvent.isCancelled()) {
            return;
        }

        runtime.setLastPlayerId(player.getUniqueId());
        player.openInventory(runtime.inventory());
    }

    private boolean matchesMaterial(String configured, Material actual) {

        if (configured == null || configured.isBlank()) {
            return false;
        }

        try {
            return Material.valueOf(configured.trim().toUpperCase(Locale.ROOT)) == actual;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}

package com.sack.rpgroll.furniture.placed;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Pregunta a los plugins de protección si alguien puede poner o quitar un mueble aquí.
 * <p>
 * No hay una API común para WorldGuard, GriefPrevention, Towny o los territorios de guild, pero
 * todos escuchan {@link BlockPlaceEvent} y {@link BlockBreakEvent}: se lanza uno de prueba por
 * casilla y se mira si alguien lo cancela. El bloque no cambia. Mientras dura la prueba
 * {@link #testing()} es true, para que los propios listeners del módulo no reaccionen.
 */
public final class Protection {

    private static final ThreadLocal<Boolean> TESTING = ThreadLocal.withInitial(() -> false);

    private Protection() {
    }

    public static boolean testing() {
        return TESTING.get();
    }

    public static boolean canPlace(Player player, List<Block> cells, ItemStack item) {

        TESTING.set(true);
        try {
            for (Block cell : cells) {
                BlockPlaceEvent event = new BlockPlaceEvent(cell, cell.getState(), cell.getRelative(BlockFace.DOWN),
                        item == null ? new ItemStack(Material.BARRIER) : item, player, true, EquipmentSlot.HAND);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled() || !event.canBuild()) {
                    return false;
                }
            }
            return true;
        } finally {
            TESTING.set(false);
        }
    }

    public static boolean canBreak(Player player, Block cell) {

        TESTING.set(true);
        try {
            BlockBreakEvent event = new BlockBreakEvent(cell, player);
            event.setDropItems(false);
            event.setExpToDrop(0);
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } finally {
            TESTING.set(false);
        }
    }
}

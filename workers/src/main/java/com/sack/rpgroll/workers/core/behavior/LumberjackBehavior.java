package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.LivingEntity;

import java.util.Locale;

/**
 * Corta el tronco más cercano — no reforesta de verdad (plantar un
 * árbol completo de nuevo requiere sapling correcto + espacio + luz,
 * mucho más que resetear la edad de un cultivo como hace el granjero),
 * así que "Replantan" del diseño original queda deliberadamente afuera
 * de esta pasada.
 */
public class LumberjackBehavior implements ProfessionBehavior {

    private static final int MAX_TRUNK_HEIGHT = 32;

    private final double searchRadius;

    public LumberjackBehavior(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    @Override
    public void work(Worker worker, LivingEntity entity, Profession profession) {

        if (worker.isInventoryFull()) {
            return;
        }

        Location origin = entity.getLocation();
        Block log = findNearestLog(worker, WorkSite.anchor(worker, entity));

        if (log == null) {
            return;
        }

        if (origin.distance(log.getLocation()) > 2.5) {
            MovementUtil.moveToward(entity, log.getLocation());
            return;
        }

        if (!WorkSite.mayChange(worker, entity, log, Material.AIR.createBlockData())) {
            return;
        }

        Material logType = log.getType();
        log.setType(Material.AIR);
        worker.addCarried(logType.name(), 1);
    }

    private Block findNearestLog(Worker worker, Location origin) {

        World world = origin.getWorld();

        if (world == null) {
            return null;
        }

        int radius = (int) searchRadius;
        Block nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {

                if (!WorkSite.isLoaded(world, baseX + x, baseZ + z)) {
                    continue;
                }

                for (int y = -3; y <= radius; y++) {

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (!isLog(block.getType()) || !isTreeTrunk(block) || !WorkSite.mayWorkAt(worker, block)) {
                        continue;
                    }

                    double distance = block.getLocation().distanceSquared(origin);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = block;
                    }
                }
            }
        }

        return nearest;
    }

    private boolean isLog(Material material) {
        return material.name().toLowerCase(Locale.ROOT).endsWith("_log");
    }

    /**
     * Un tronco de árbol y no la pared de una casa: sube por los troncos y
     * busca hojas naturales (las que pone un jugador son persistentes) en lo
     * alto de la columna. Antes talaba cualquier bloque *_log del radio.
     */
    private boolean isTreeTrunk(Block log) {

        Block top = log;

        for (int i = 0; i < MAX_TRUNK_HEIGHT && isLog(top.getRelative(BlockFace.UP).getType()); i++) {
            top = top.getRelative(BlockFace.UP);
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (top.getRelative(dx, dy, dz).getBlockData() instanceof Leaves leaves && !leaves.isPersistent()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}

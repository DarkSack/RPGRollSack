package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
        Block log = findNearestLog(origin);

        if (log == null) {
            return;
        }

        if (origin.distance(log.getLocation()) > 2.5) {
            MovementUtil.moveToward(entity, log.getLocation());
            return;
        }

        Material logType = log.getType();
        log.setType(Material.AIR);
        worker.addCarried(logType.name(), 1);
    }

    private Block findNearestLog(Location origin) {

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
            for (int y = -3; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (!isLog(block.getType())) {
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

}

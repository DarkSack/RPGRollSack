package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.Random;

/**
 * El pescador busca agua cercana y "pesca" un pez vanilla genérico
 * mientras está parado en la orilla. Deliberadamente NO usa
 * {@code FishingAPI.forceCatch(...)} — ese método exige un
 * {@code Player} real (lo necesita para chequear nivel/RPGRollAPI en
 * peces legendarios), y un worker no es un jugador. Simularlo con un
 * jugador falso sería fràgil y engañoso, así que este behavior se queda
 * con una captura vanilla simple en vez de la genética/calidad rica de
 * RPGRoll-Fishing.
 */
public class FishingBehavior implements ProfessionBehavior {

    private static final Material[] CATCHES = { Material.COD, Material.SALMON, Material.TROPICAL_FISH, Material.PUFFERFISH };

    private final double searchRadius;
    private final Random random = new Random();

    public FishingBehavior(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    @Override
    public void work(Worker worker, LivingEntity entity, Profession profession) {

        if (worker.isInventoryFull()) {
            return;
        }

        Location origin = entity.getLocation();
        Location water = findNearestWater(origin);

        if (water == null) {
            return;
        }

        if (origin.distance(water) > 2.5) {
            MovementUtil.moveToward(entity, water);
            return;
        }

        if (random.nextDouble() < 0.3) {
            worker.addCarried(CATCHES[random.nextInt(CATCHES.length)].name(), 1);
        }
    }

    private Location findNearestWater(Location origin) {

        World world = origin.getWorld();

        if (world == null) {
            return null;
        }

        int radius = (int) searchRadius;
        Location nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        int baseX = origin.getBlockX();
        int baseZ = origin.getBlockZ();
        int baseY = origin.getBlockY();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {

                    var block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (block.getType() != Material.WATER) {
                        continue;
                    }

                    double distance = block.getLocation().distanceSquared(origin);

                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = block.getLocation();
                    }
                }
            }
        }

        return nearest;
    }

}

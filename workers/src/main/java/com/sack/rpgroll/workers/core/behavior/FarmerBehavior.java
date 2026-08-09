package com.sack.rpgroll.workers.core.behavior;

import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.worker.Worker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/** Cosecha el cultivo maduro más cercano y lo replanta al instante — siembra/riego real de tierra no están modelados. */
public class FarmerBehavior implements ProfessionBehavior {

    private static final Map<Material, Material> HARVEST_YIELD = Map.of(
            Material.WHEAT, Material.WHEAT,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT);

    private final double searchRadius;

    public FarmerBehavior(double searchRadius) {
        this.searchRadius = searchRadius;
    }

    @Override
    public void work(Worker worker, LivingEntity entity, Profession profession) {

        if (worker.isInventoryFull()) {
            return;
        }

        Location origin = entity.getLocation();
        Block crop = findMatureCrop(origin);

        if (crop == null) {
            return;
        }

        if (origin.distance(crop.getLocation()) > 2.5) {
            MovementUtil.moveToward(entity, crop.getLocation());
            return;
        }

        Material yield = HARVEST_YIELD.get(crop.getType());

        if (yield == null) {
            return;
        }

        worker.addCarried(yield.name(), 1 + (int) (Math.random() * 2));

        if (crop.getBlockData() instanceof Ageable ageable) {
            ageable.setAge(0);
            crop.setBlockData(ageable);
        }
    }

    private Block findMatureCrop(Location origin) {

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
                for (int y = -2; y <= 2; y++) {

                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);

                    if (!HARVEST_YIELD.containsKey(block.getType())) {
                        continue;
                    }

                    if (!(block.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
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

}

package com.sack.rpgroll.seasons.vegetation;

import com.sack.rpgroll.seasons.climate.ClimateManager;
import com.sack.rpgroll.seasons.core.Season;
import com.sack.rpgroll.seasons.core.VegetationEffectType;
import com.sack.rpgroll.seasons.runtime.RegionSeasonResolver;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;

/**
 * Aplica los {@link VegetationEffectType} de la estación activa cerca de
 * cada jugador online — muestras al azar, con probabilidad baja por
 * intento, para que sea un efecto ambiental gradual y no un "photoshop"
 * instantáneo de todo el radio de golpe.
 */
public class VegetationTask implements Runnable {

    private static final int RADIUS = 10;
    private static final int SAMPLES_PER_PLAYER = 4;
    private static final double ACTION_CHANCE = 0.15;

    private static final Set<Material> SNOWABLE = Set.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL, Material.STONE,
            Material.SAND, Material.GRAVEL);

    private final RegionSeasonResolver regionResolver;
    private final ClimateManager climateManager;
    private final Random random = new Random();

    public VegetationTask(RegionSeasonResolver regionResolver, ClimateManager climateManager) {
        this.regionResolver = regionResolver;
        this.climateManager = climateManager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayer(player);
        }
    }

    private void processPlayer(Player player) {

        Season season = regionResolver.resolveSeason(player.getLocation()).orElse(null);

        if (season == null || season.vegetationEffects().isEmpty()) {
            return;
        }

        for (int i = 0; i < SAMPLES_PER_PLAYER; i++) {

            Location sample = randomNearby(player.getLocation());

            for (VegetationEffectType effect : season.vegetationEffects()) {
                if (random.nextDouble() < ACTION_CHANCE) {
                    applyEffect(effect, sample);
                }
            }
        }
    }

    private Location randomNearby(Location origin) {

        int dx = random.nextInt(RADIUS * 2 + 1) - RADIUS;
        int dz = random.nextInt(RADIUS * 2 + 1) - RADIUS;
        Location base = origin.clone().add(dx, 0, dz);
        int topY = base.getWorld().getHighestBlockYAt(base);

        return new Location(base.getWorld(), base.getX(), topY, base.getZ());
    }

    private void applyEffect(VegetationEffectType effect, Location sample) {
        switch (effect) {
            case SNOW_LAYERS -> trySnow(sample);
            case ICE_LAKES -> tryFreeze(sample);
            case DRY_GRASS -> tryDryFarmland(sample);
            case FALLING_LEAVES -> tryFallingLeaves(sample);
            case FLOWER_BOOM -> tryFlowerBoom(sample);
        }
    }

    private void trySnow(Location sample) {

        if (climateManager.getTemperature(sample) >= 0) {
            return;
        }

        Block ground = sample.getBlock();
        Block above = ground.getRelative(0, 1, 0);

        if (SNOWABLE.contains(ground.getType()) && above.getType().isAir()) {
            above.setType(Material.SNOW);
        }
    }

    private void tryFreeze(Location sample) {

        Block block = sample.getBlock();

        if (block.getType() == Material.WATER && climateManager.getTemperature(sample) < 0) {
            block.setType(Material.ICE);
        }
    }

    private void tryDryFarmland(Location sample) {

        Block block = sample.getBlock();

        if (block.getBlockData() instanceof Farmland farmland && farmland.getMoisture() > 0) {
            farmland.setMoisture(farmland.getMoisture() - 1);
            block.setBlockData(farmland);
        }
    }

    private void tryFallingLeaves(Location sample) {

        Block ground = sample.getBlock();
        Block above = ground.getRelative(0, 2, 0);

        if (above.getType().name().endsWith("_LEAVES")) {
            ground.getWorld().spawnParticle(Particle.CHERRY_LEAVES, above.getLocation(), 3, 0.4, 0.2, 0.4, 0);
        }
    }

    private void tryFlowerBoom(Location sample) {

        Block ground = sample.getBlock();
        Block above = ground.getRelative(0, 1, 0);

        if (ground.getType() == Material.GRASS_BLOCK && above.getType().isAir()) {
            Material[] flowers = { Material.DANDELION, Material.POPPY, Material.AZURE_BLUET, Material.CORNFLOWER };
            above.setType(flowers[random.nextInt(flowers.length)]);
        }
    }

}

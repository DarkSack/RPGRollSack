package com.sack.rpgroll.extras.temperature;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Temperatura ambiental (sección 6) en una escala aproximada a °C: bioma
 * (vía {@code Block#getTemperature()} vanilla, que ya cubre CUALQUIER
 * bioma sin mantener una tabla propia) + hora + clima + altitud +
 * dimensión + bloques cercanos (lava/fuego calientan, hielo/nieve enfrían).
 */
public class AmbientTemperatureCalculator {

    private static final double SEA_LEVEL = 64;
    private static final int NEARBY_RADIUS = 3;

    public double calculate(Player player) {

        Location location = player.getLocation();

        double celsius = location.getBlock().getTemperature() * 20;

        celsius += timeOfDayModifier(player.getWorld());
        celsius += weatherModifier(player.getWorld());
        celsius += altitudeModifier(location);
        celsius += dimensionModifier(player.getWorld().getEnvironment());
        celsius += nearbyBlocksModifier(location);

        return celsius;
    }

    private double timeOfDayModifier(World world) {

        long time = world.getTime();
        boolean night = time >= 13000 && time <= 23000;

        return night ? -5 : 0;
    }

    private double weatherModifier(World world) {

        if (world.isThundering()) {
            return -8;
        }

        if (world.hasStorm()) {
            return -5;
        }

        return 0;
    }

    private double altitudeModifier(Location location) {
        return -((location.getY() - SEA_LEVEL) / 16.0);
    }

    private double dimensionModifier(World.Environment environment) {

        return switch (environment) {
            case NETHER -> 25;
            case THE_END -> -10;
            default -> 0;
        };
    }

    private double nearbyBlocksModifier(Location center) {

        double modifier = 0;

        for (int dx = -NEARBY_RADIUS; dx <= NEARBY_RADIUS; dx++) {
            for (int dy = -NEARBY_RADIUS; dy <= NEARBY_RADIUS; dy++) {
                for (int dz = -NEARBY_RADIUS; dz <= NEARBY_RADIUS; dz++) {

                    Material type = center.clone().add(dx, dy, dz).getBlock().getType();

                    if (isHeatSource(type)) {
                        modifier += 3;
                    } else if (isColdSource(type)) {
                        modifier -= 2;
                    }
                }
            }
        }

        return Math.max(-15, Math.min(20, modifier));
    }

    private boolean isHeatSource(Material type) {
        return type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE
                || type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE || type == Material.MAGMA_BLOCK
                || type == Material.LAVA_CAULDRON;
    }

    private boolean isColdSource(Material type) {
        return type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE
                || type == Material.SNOW || type == Material.SNOW_BLOCK || type == Material.POWDER_SNOW;
    }

}

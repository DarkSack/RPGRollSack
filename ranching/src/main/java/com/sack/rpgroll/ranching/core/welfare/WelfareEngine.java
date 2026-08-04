package com.sack.rpgroll.ranching.core.welfare;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Calcula la felicidad "objetivo" de un animal a partir de su entorno —
 * {@link com.sack.rpgroll.ranching.core.welfare.WelfareTask} después
 * acerca la felicidad real a este objetivo gradualmente, en vez de
 * saltar de golpe.
 * <p>
 * Simplificación deliberada de "limpieza": Bukkit no expone ninguna
 * noción de suciedad de un corral, así que ese factor del diseño
 * original se reemplaza por luz + agua cercana + hacinamiento, que sí
 * son medibles de verdad contra el mundo real.
 */
public class WelfareEngine {

    private static final int WATER_SCAN_RADIUS = 3;
    private static final int COMPANIONSHIP_MAX = 5;
    private static final int OVERCROWD_MIN = 9;

    /** @param ambientTemperature temperatura de RPGRoll-Seasons en esa ubicación, o null si el addon no está activo. */
    public double computeTargetHappiness(Location location, int nearbySameSpeciesCount, double health,
            Double ambientTemperature) {

        double score = 60;

        int light = location.getBlock().getLightLevel();
        score += light >= 8 ? 10 : -10;

        score += hasNearbyWater(location) ? 5 : -2;

        if (nearbySameSpeciesCount <= 0) {
            score -= 10;
        } else if (nearbySameSpeciesCount <= COMPANIONSHIP_MAX) {
            score += 10;
        } else if (nearbySameSpeciesCount >= OVERCROWD_MIN) {
            score -= 15;
        }

        if (ambientTemperature != null && (ambientTemperature < -10 || ambientTemperature > 35)) {
            score -= 15;
        }

        if (health < 50) {
            score -= 10;
        }

        return Math.max(0, Math.min(100, score));
    }

    private boolean hasNearbyWater(Location location) {

        World world = location.getWorld();

        if (world == null) {
            return false;
        }

        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        for (int x = -WATER_SCAN_RADIUS; x <= WATER_SCAN_RADIUS; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -WATER_SCAN_RADIUS; z <= WATER_SCAN_RADIUS; z++) {

                    Material type = world.getBlockAt(baseX + x, baseY + y, baseZ + z).getType();

                    if (type == Material.WATER) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

}

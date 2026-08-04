package com.sack.rpgroll.fishing.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Una caja (AABB, sin WorldGuard) que fuerza un {@link WaterType} —
 * necesaria para {@code MAGIC_WATER}/{@code CORRUPTED_WATER}, ya que
 * ningún bioma vanilla las implica. Mismo estilo simple de bounds que
 * {@code SeasonRegion} de RPGRoll-Seasons.
 */
public record FishingRegion(String id, String world, double minX, double minY, double minZ, double maxX,
        double maxY, double maxZ, WaterType forcedWaterType) implements RPGContent {

    public FishingRegion {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
        forcedWaterType = forcedWaterType == null ? WaterType.MAGIC_WATER : forcedWaterType;
    }

    public boolean contains(String worldName, double x, double y, double z) {
        return world.equalsIgnoreCase(worldName)
                && x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                && y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
                && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
    }

}

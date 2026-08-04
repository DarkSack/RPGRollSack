package com.sack.rpgroll.seasons.core;

import com.sack.rpgroll.common.content.RPGContent;

import java.util.Objects;

/**
 * Una caja (AABB) donde la estación efectiva no es necesariamente la del
 * mundo — mismo estilo simple de bounds que {@code DungeonRoom} de
 * RPGRoll-Dungeons, sin depender de WorldGuard.
 */
public record SeasonRegion(
        String id,
        String world,
        double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ,
        SeasonRegionOverrideMode overrideMode,
        String pinnedSeasonId,
        String pinnedCalendarId) implements RPGContent {

    public SeasonRegion {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
        overrideMode = overrideMode == null ? SeasonRegionOverrideMode.FOLLOW_WORLD_CALENDAR : overrideMode;
        pinnedSeasonId = (pinnedSeasonId == null || pinnedSeasonId.isBlank()) ? null : pinnedSeasonId;
        pinnedCalendarId = (pinnedCalendarId == null || pinnedCalendarId.isBlank()) ? null : pinnedCalendarId;
    }

    public boolean contains(String worldName, double x, double y, double z) {
        return world.equalsIgnoreCase(worldName)
                && x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                && y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
                && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
    }

    /** Clave única de reloj para PINNED_CALENDAR — nunca colisiona con "world:&lt;nombre&gt;". */
    public String clockKey() {
        return "region:" + id;
    }

}

package com.sack.rpgroll.mobs.region;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Location;

import java.util.Objects;

/**
 * Región cuboide simple (sin dependencia de WorldGuard) usada por reglas
 * de spawn y por la IA de "proteger región"/"guardián".
 */
public record MobRegion(String id, String world, double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ) implements RPGContent {

    public MobRegion {
        Objects.requireNonNull(id, "id no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id no puede estar vacío");
        }

        if (minX > maxX) {
            double tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minY > maxY) {
            double tmp = minY;
            minY = maxY;
            maxY = tmp;
        }
        if (minZ > maxZ) {
            double tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }
    }

    public boolean contains(Location location) {

        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }

        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public Location center() {

        var world = org.bukkit.Bukkit.getWorld(this.world);
        return new Location(world, (minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

}

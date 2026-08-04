package com.sack.rpgroll.quests.region;

import com.sack.rpgroll.common.content.RPGContent;

import org.bukkit.Location;

import java.util.Objects;

/**
 * Región cuboide simple (sin dependencia de WorldGuard u otro plugin de
 * regiones) usada por el requisito "region" y el objetivo DISCOVER_REGION.
 * Suficiente para marcar zonas de interés en el mundo sin traer una
 * dependencia externa nueva solo para esto.
 */
public record Region(String id, String world, double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ) implements RPGContent {

    public Region {
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

}

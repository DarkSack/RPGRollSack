package com.sack.rpgroll.dungeons.core;

import org.bukkit.Location;

/**
 * Cuboide simple (sin WorldGuard) que delimita el área física de una
 * dungeon — se usa para saber si un jugador sigue adentro (abandono por
 * salir caminando) y como referencia de "está libre/ocupada" ya que, por
 * decisión de alcance, una dungeon física solo tiene una corrida activa
 * a la vez (sin clonado de estructura).
 */
public record DungeonBounds(String world, double minX, double minY, double minZ,
        double maxX, double maxY, double maxZ) {

    public DungeonBounds {
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

    public static DungeonBounds none() {
        return new DungeonBounds("world", 0, 0, 0, 0, 0, 0);
    }

    public boolean contains(Location location) {

        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }

        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public DungeonPoint center() {
        return new DungeonPoint(world, (minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2, 0, 0);
    }

}

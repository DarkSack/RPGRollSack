package com.sack.rpgroll.structures.storage;

/** Caja de bloques, extremos incluidos. */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public boolean intersects(Box o) {
        return minX <= o.maxX && maxX >= o.minX && minY <= o.maxY && maxY >= o.minY && minZ <= o.maxZ
                && maxZ >= o.minZ;
    }
}

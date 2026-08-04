package com.sack.rpgroll.guilds.guild.territory;

import org.bukkit.Location;

/**
 * Región reclamada por una guild — cuboide simple sin WorldGuard (decisión
 * de alcance confirmada), igual patrón que {@code DungeonBounds}/{@code
 * MobRegion}/{@code Region} de Quests. Protecciones básicas: bloqueo de
 * romper/colocar bloques y PvP para no-miembros, controlable por flags.
 */
public class GuildTerritory {

    private final String name;
    private String world;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    private boolean protectBlocks = true;
    private boolean allowOutsiderPvp = false;

    public GuildTerritory(String name, String world, double x1, double y1, double z1, double x2, double y2,
            double z2) {
        this.name = name;
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String name() {
        return name;
    }

    public String world() {
        return world;
    }

    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    public boolean protectBlocks() {
        return protectBlocks;
    }

    public void setProtectBlocks(boolean protectBlocks) {
        this.protectBlocks = protectBlocks;
    }

    public boolean allowOutsiderPvp() {
        return allowOutsiderPvp;
    }

    public void setAllowOutsiderPvp(boolean allowOutsiderPvp) {
        this.allowOutsiderPvp = allowOutsiderPvp;
    }

    public double volume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    public boolean contains(Location location) {

        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }

        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    /** @return true si este territorio se superpone con el cuboide dado (mismo mundo). */
    public boolean overlaps(String otherWorld, double ox1, double oy1, double oz1, double ox2, double oy2,
            double oz2) {

        if (!world.equals(otherWorld)) {
            return false;
        }

        double ominX = Math.min(ox1, ox2), omaxX = Math.max(ox1, ox2);
        double ominY = Math.min(oy1, oy2), omaxY = Math.max(oy1, oy2);
        double ominZ = Math.min(oz1, oz2), omaxZ = Math.max(oz1, oz2);

        return minX <= omaxX && maxX >= ominX
                && minY <= omaxY && maxY >= ominY
                && minZ <= omaxZ && maxZ >= ominZ;
    }

}

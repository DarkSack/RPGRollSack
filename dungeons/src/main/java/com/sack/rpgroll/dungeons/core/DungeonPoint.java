package com.sack.rpgroll.dungeons.core;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Un punto con orientación (mundo/x/y/z/yaw/pitch) — lobby, entrada de sala, spawn de jefe, etc. */
public record DungeonPoint(String world, double x, double y, double z, float yaw, float pitch) {

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public static DungeonPoint fromLocation(Location location) {
        return new DungeonPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

}

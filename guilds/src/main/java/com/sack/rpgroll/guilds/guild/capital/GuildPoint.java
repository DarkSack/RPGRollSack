package com.sack.rpgroll.guilds.guild.capital;

import org.bukkit.Bukkit;
import org.bukkit.Location;

/** Un punto con orientación (mundo/x/y/z/yaw/pitch) — spawn, banco, teleport nombrado de la capital. */
public record GuildPoint(String world, double x, double y, double z, float yaw, float pitch) {

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
    }

    public static GuildPoint fromLocation(Location location) {
        return new GuildPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
    }

}

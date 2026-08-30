package com.sack.rpgroll.traps.turret;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * Una ubicación física donde se colocó una torreta — a diferencia de
 * {@code PlacedTrap}, no tiene state machine propia (una torreta siempre
 * está "armada"; su objetivo/cooldown de disparo es estado de runtime en
 * memoria, ver {@link TurretEngine}, no algo que valga la pena persistir).
 */
public record PlacedTurret(String placementId, String turretId, String world, int x, int y, int z) {

    public PlacedTurret {
        Objects.requireNonNull(placementId, "placementId no puede ser null");
        Objects.requireNonNull(turretId, "turretId no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
    }

    public Location location() {
        World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x + 0.5, y + 0.5, z + 0.5);
    }

}

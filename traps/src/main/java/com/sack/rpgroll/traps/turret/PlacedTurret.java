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
public record PlacedTurret(String placementId, String turretId, String world, int x, int y, int z,
        java.util.UUID owner,
        java.util.Map<String, Integer> ammo,
        TurretTargeting targeting) {

    /**
     * @param owner quién la colocó. Solo esa persona (o un admin) puede
     *              retirarla. Null en las colocadas por comando antes de que
     *              existiera el ítem: esas quedan sin dueño y solo las puede
     *              sacar un admin.
     */
    public PlacedTurret {
        // Copia defensiva: el motor consume munición y no debe poder mutar
        // el estado guardado sin pasar por el manager.
        ammo = ammo == null ? java.util.Map.of() : java.util.Map.copyOf(ammo);
        Objects.requireNonNull(placementId, "placementId no puede ser null");
        Objects.requireNonNull(turretId, "turretId no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
    }

    public Location location() {
        World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x + 0.5, y + 0.5, z + 0.5);
    }

}

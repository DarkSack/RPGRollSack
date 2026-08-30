package com.sack.rpgroll.traps.location;

import com.sack.rpgroll.traps.core.TrapState;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.Optional;

/**
 * Una ubicación física en el mundo donde se registró una trampa, más su
 * estado de instancia (state machine, cargas restantes, cooldown). Soporta
 * dos formas: punto único (x2/y2/z2 nulos) o zona cuboide (para trampas de
 * área, ej. sala de gas) — mismo espíritu que PlacedCrate pero con estado
 * mutable propio, ya que cada instancia recorre IDLE/ARMED/TRIGGERED/
 * COOLDOWN/DEPLETED de forma independiente.
 */
public record PlacedTrap(
        String placementId,
        String trapId,
        String world,
        int x,
        int y,
        int z,
        Integer x2,
        Integer y2,
        Integer z2,
        TrapState state,
        int chargesRemaining,
        long cooldownExpiresAtMillis) {

    public PlacedTrap {
        Objects.requireNonNull(placementId, "placementId no puede ser null");
        Objects.requireNonNull(trapId, "trapId no puede ser null");
        Objects.requireNonNull(world, "world no puede ser null");
        state = state == null ? TrapState.ARMED : state;
    }

    public boolean isZone() {
        return x2 != null && y2 != null && z2 != null;
    }

    /** Bloque de referencia (esquina 1 en zonas, único punto si no lo es). */
    public Location primaryLocation() {
        World bukkitWorld = org.bukkit.Bukkit.getWorld(world);
        return bukkitWorld == null ? null : new Location(bukkitWorld, x, y, z);
    }

    public boolean contains(Location location) {

        if (location.getWorld() == null || !location.getWorld().getName().equals(world)) {
            return false;
        }

        if (!isZone()) {
            return location.getBlockX() == x && location.getBlockY() == y && location.getBlockZ() == z;
        }

        int minX = Math.min(x, x2);
        int maxX = Math.max(x, x2);
        int minY = Math.min(y, y2);
        int maxY = Math.max(y, y2);
        int minZ = Math.min(z, z2);
        int maxZ = Math.max(z, z2);

        int bx = location.getBlockX();
        int by = location.getBlockY();
        int bz = location.getBlockZ();

        return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
    }

    public PlacedTrap withState(TrapState newState) {
        return new PlacedTrap(placementId, trapId, world, x, y, z, x2, y2, z2, newState, chargesRemaining,
                cooldownExpiresAtMillis);
    }

    public PlacedTrap withCharges(int newCharges) {
        return new PlacedTrap(placementId, trapId, world, x, y, z, x2, y2, z2, state, newCharges,
                cooldownExpiresAtMillis);
    }

    public PlacedTrap withCooldownExpiresAt(long millis) {
        return new PlacedTrap(placementId, trapId, world, x, y, z, x2, y2, z2, state, chargesRemaining, millis);
    }

    public static Optional<PlacedTrap> tryFind(Iterable<PlacedTrap> all, Location location) {
        for (PlacedTrap placed : all) {
            if (placed.contains(location)) {
                return Optional.of(placed);
            }
        }
        return Optional.empty();
    }

}

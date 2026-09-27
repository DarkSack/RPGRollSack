package com.sack.rpgroll.furniture.seat;

import com.sack.rpgroll.furniture.FurnitureKeys;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.Offset;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Seat;
import com.sack.rpgroll.furniture.placed.PlacedFurniture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

/**
 * Sentarse en un mueble.
 * <p>
 * El jugador monta un ArmorStand marcador e invisible que existe solo mientras está sentado
 * (no se guarda con el chunk). Geyser traduce bien esa montura a Bedrock, y la extensión
 * GeyserDisplayEntity sabe corregir su altura para esos clientes.
 * <p>
 * Un jugador que monta algo queda con los pies 0,6 bloques por debajo del punto de monta, que
 * en un marcador es su propia posición: poner el soporte a la altura del asiento deja la cadera
 * justo encima.
 */
public class SeatService {

    /** Quién ocupa qué: soporte → (mueble, plaza). */
    private record Taken(UUID furniture, int seat) {
    }

    private final FurnitureKeys keys;
    private final DoubleSupplier heightOffset;
    private final Map<UUID, Taken> seats = new ConcurrentHashMap<>();

    public SeatService(FurnitureKeys keys, DoubleSupplier heightOffset) {
        this.keys = keys;
        this.heightOffset = heightOffset;
    }

    /** Sienta al jugador en la plaza libre más cercana. False si están todas ocupadas. */
    public boolean sit(Player player, PlacedFurniture furniture, FurnitureDefinition def) {

        Seat seat = def.functions().seat();
        if (seat == null || player.isInsideVehicle()) {
            return false;
        }

        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        List<Offset> positions = seat.positions();

        for (int i = 0; i < positions.size(); i++) {
            if (occupied(furniture.uuid(), i)) {
                continue;
            }
            double distance = seatLocation(furniture, seat, i).distanceSquared(player.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }

        if (best < 0) {
            return false;
        }

        Location at = seatLocation(furniture, seat, best);
        int seatIndex = best;

        ArmorStand stand = at.getWorld().spawn(at, ArmorStand.class, s -> {
            s.setMarker(true);
            s.setInvisible(true);
            s.setSmall(true);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setSilent(true);
            s.setPersistent(false);
            s.setCanTick(false);
            s.getPersistentDataContainer().set(keys.seat, PersistentDataType.BYTE, (byte) 1);
            s.getPersistentDataContainer().set(keys.parent, PersistentDataType.STRING, furniture.uuid().toString());
        });

        seats.put(stand.getUniqueId(), new Taken(furniture.uuid(), seatIndex));

        Location facing = player.getLocation();
        facing.setYaw(furniture.yaw());
        facing.setPitch(0);
        player.setRotation(facing.getYaw(), facing.getPitch());

        if (!stand.addPassenger(player)) {
            seats.remove(stand.getUniqueId());
            stand.remove();
            return false;
        }
        return true;
    }

    private Location seatLocation(PlacedFurniture furniture, Seat seat, int index) {

        Offset o = seat.positions().get(index).rotate(furniture.yaw());
        Block anchor = furniture.anchor();
        Location at = anchor.getLocation().add(0.5 + o.x(), o.y() + seat.height() + heightOffset.getAsDouble(), 0.5 + o.z());
        at.setYaw(furniture.yaw());
        return at;
    }

    private boolean occupied(UUID furniture, int seat) {

        for (Map.Entry<UUID, Taken> entry : seats.entrySet()) {
            if (entry.getValue().furniture().equals(furniture) && entry.getValue().seat() == seat) {
                Entity stand = Bukkit.getEntity(entry.getKey());
                if (stand != null && stand.isValid() && !stand.getPassengers().isEmpty()) {
                    return true;
                }
                // Soporte perdido (el jugador se fue sin evento de bajada): la plaza está libre.
                seats.remove(entry.getKey());
                if (stand != null) {
                    stand.remove();
                }
            }
        }
        return false;
    }

    public boolean isSeat(Entity entity) {
        return entity instanceof ArmorStand && entity.getPersistentDataContainer().has(keys.seat);
    }

    /** Al bajarse: se quita el soporte y, si quedó dentro de una barrera, se le sube encima. */
    public void onDismount(Player player, Entity stand) {

        seats.remove(stand.getUniqueId());
        Location standAt = stand.getLocation();
        stand.remove();

        Location out = player.getLocation();
        Block feet = out.getBlock();
        if (feet.getType() == org.bukkit.Material.BARRIER || standAt.getBlock().getType() == org.bukkit.Material.BARRIER) {
            Block above = standAt.getBlock();
            while (above.getType() == org.bukkit.Material.BARRIER && above.getY() < above.getWorld().getMaxHeight() - 1) {
                above = above.getRelative(0, 1, 0);
            }
            Location safe = above.getLocation().add(0.5, 0, 0.5);
            safe.setYaw(out.getYaw());
            safe.setPitch(out.getPitch());
            player.teleport(safe);
        }
    }

    /** Baja a todos los sentados en este mueble (al retirarlo o girarlo). */
    public void ejectAll(PlacedFurniture furniture) {

        seats.entrySet().removeIf(entry -> {
            if (!entry.getValue().furniture().equals(furniture.uuid())) {
                return false;
            }
            Entity stand = Bukkit.getEntity(entry.getKey());
            if (stand != null) {
                stand.eject();
                stand.remove();
            }
            return true;
        });
    }

    /** Al apagar: nadie se queda montado en un soporte que ya no existe. */
    public void clearAll() {

        seats.keySet().forEach(id -> {
            Entity stand = Bukkit.getEntity(id);
            if (stand != null) {
                stand.eject();
                stand.remove();
            }
        });
        seats.clear();

        // Por si quedó alguno de un cierre brusco (no se guardan, pero un /reload los deja vivos).
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
                if (isSeat(stand)) {
                    stand.eject();
                    stand.remove();
                }
            }
        }
    }
}

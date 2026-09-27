package com.sack.rpgroll.furniture.listener;

import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.core.Offset;
import com.sack.rpgroll.furniture.function.FurnitureFunctions.Ambient;
import com.sack.rpgroll.furniture.placed.FurnitureIndex;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * Las partículas de ambiente (humo de chimenea, llama de vela). Solo se calculan para los muebles
 * con algún jugador cerca, y sin buscar entidades: todo sale del índice.
 */
public class AmbientTask implements Runnable {

    public static final long PERIOD = 2L;

    private final FurnitureIndex index;
    private final FurnitureManager manager;
    private final DoubleSupplier range;
    private long tick;

    public AmbientTask(FurnitureIndex index, FurnitureManager manager, DoubleSupplier range) {
        this.index = index;
        this.manager = manager;
        this.range = range;
    }

    @Override
    public void run() {

        tick += PERIOD;
        double max = range.getAsDouble();
        double maxSquared = max * max;

        for (FurnitureIndex.Entry entry : index.entries()) {

            Ambient ambient = manager.get(entry.furnitureId()).map(d -> d.functions().ambient()).orElse(null);
            if (ambient == null || tick % ambient.everyTicks() >= PERIOD) {
                continue;
            }
            if (ambient.state() >= 0 && ambient.state() != entry.state()) {
                continue;
            }

            World world = Bukkit.getWorld(entry.anchor().world());
            if (world == null) {
                continue;
            }

            Location at = new Location(world, entry.anchor().x() + 0.5, entry.anchor().y(), entry.anchor().z() + 0.5);
            List<Player> players = world.getPlayers();
            if (players.stream().noneMatch(p -> p.getLocation().distanceSquared(at) <= maxSquared)) {
                continue;
            }

            Offset o = ambient.at().rotate(entry.yaw());
            at.add(o.x(), o.y(), o.z());

            Particle particle = particle(ambient.particle());
            if (particle != null && particle.getDataType() == Void.class) {
                world.spawnParticle(particle, at, ambient.count(), ambient.spread(), ambient.spread(), ambient.spread(), 0);
            }
        }
    }

    private static Particle particle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.sack.rpgroll.structures.project;

import com.sack.rpgroll.structures.build.BlockWriter;
import com.sack.rpgroll.structures.build.PlacementPlan;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Lecturas del mundo para reparar, repartidas en ticks ({@value #BLOCKS_PER_TICK} posiciones por tick y lectura, como
 * la validación completa de la vista previa) y una a la vez por jugador. Todo en el hilo principal; {@link #tick} cada
 * tick del servidor.
 */
public final class RepairScans {

    static final int BLOCKS_PER_TICK = 4096;

    private record Pending(RepairMath.Scan scan, CompletableFuture<RepairMath.Result> done) {
    }

    private final BlockWriter writer;
    private final Map<UUID, Pending> running = new LinkedHashMap<>();

    public RepairScans(BlockWriter writer) {
        this.writer = writer;
    }

    /**
     * Empieza a contar lo que falta en la caja del plan con esquina mínima (ox, oy, oz). Vacío si ese jugador ya tiene
     * una lectura en curso (no se encolan clics). El futuro termina en {@link #tick}.
     */
    public Optional<CompletableFuture<RepairMath.Result>> start(UUID player, PlacementPlan plan, Set<Material> replace,
                                                                String world, int ox, int oy, int oz) {
        if (running.containsKey(player)) {
            return Optional.empty();
        }
        Pending p = new Pending(new RepairMath.Scan(plan, writer, world, ox, oy, oz, replace),
                new CompletableFuture<>());
        running.put(player, p);
        return Optional.of(p.done());
    }

    public void tick() {
        // Se avisa tras el bucle: quien espera puede empezar otra lectura sin romper la iteración.
        List<Runnable> finished = new ArrayList<>();
        for (var it = running.values().iterator(); it.hasNext(); ) {
            Pending p = it.next();
            try {
                RepairMath.Result r = p.scan().step(BLOCKS_PER_TICK);
                if (r != null) {
                    it.remove();
                    finished.add(() -> p.done().complete(r));
                }
            } catch (RuntimeException e) {
                it.remove();
                finished.add(() -> p.done().completeExceptionally(e));
            }
        }
        finished.forEach(Runnable::run);
    }
}
